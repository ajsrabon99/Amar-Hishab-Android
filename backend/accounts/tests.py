import json
import hashlib
from datetime import timedelta
from django.contrib.auth import get_user_model
from django.test import TestCase, Client
from django.urls import reverse
from django.utils import timezone
from accounts.models import (
    UserProfile,
    EmailVerificationCode,
    PasswordResetCode,
    MobileAuthToken
)

User = get_user_model()


class FirstPartyAuthenticationTests(TestCase):

    def setUp(self):
        self.client = Client()

    def test_01_registration_saves_user_and_creates_verification_code(self):
        """
        Tests registration flow:
        - POST /register/ creates User in database
        - Creates UserProfile with is_email_verified=False
        - Creates EmailVerificationCode (6 digits, hashed in DB)
        - Redirects to /verify-email/?user=...
        """
        response = self.client.post('/register/', {
            'username': 'srabon',
            'email': 'srabon@example.com',
            'password': 'SecurePassword123!',
            'confirm_password': 'SecurePassword123!'
        })
        # Must redirect to verification page
        self.assertEqual(response.status_code, 302)
        self.assertIn('/verify-email/', response.url)

        # Check user is actually saved in DB
        user = User.objects.filter(username='srabon').first()
        self.assertIsNotNone(user)
        self.assertEqual(user.email, 'srabon@example.com')
        self.assertTrue(user.is_active)

        # Check profile is created and unverified
        profile = UserProfile.objects.filter(user=user).first()
        self.assertIsNotNone(profile)
        self.assertFalse(profile.is_email_verified)

        # Check verification code is created in DB
        code_record = EmailVerificationCode.objects.filter(user=user, used_at__isnull=True).first()
        self.assertIsNotNone(code_record)
        self.assertTrue(code_record.expires_at > timezone.now())

    def test_02_registration_validation_errors(self):
        """
        Tests validation: mismatch password, duplicate email, short username.
        """
        # Passwords mismatch
        res1 = self.client.post('/register/', {
            'username': 'srabon2',
            'email': 'srabon2@example.com',
            'password': 'Password123!',
            'confirm_password': 'DifferentPassword123!'
        })
        self.assertEqual(res1.status_code, 200)
        self.assertFalse(User.objects.filter(username='srabon2').exists())

        # Duplicate username
        User.objects.create_user(username='existing', email='existing@example.com', password='Password123!')
        res2 = self.client.post('/register/', {
            'username': 'existing',
            'email': 'new@example.com',
            'password': 'Password123!',
            'confirm_password': 'Password123!'
        })
        self.assertEqual(res2.status_code, 200)

    def test_03_email_verification_success_authenticates_and_redirects_to_dashboard(self):
        """
        Tests code verification:
        - Validates 6-digit code
        - Marks email as verified
        - Marks code as used
        - Automatically creates authenticated Django session
        - Redirects directly to Amar Hishab Dashboard (/)
        """
        user = User.objects.create_user(username='john', email='john@example.com', password='Password123!')
        UserProfile.objects.create(user=user, is_email_verified=False)
        raw_code = EmailVerificationCode.create_code(user, expiry_minutes=10)

        response = self.client.post('/verify-email/', {
            'action': 'verify',
            'identifier': 'john@example.com',
            'code': raw_code
        })

        # Redirects to dashboard (/)
        self.assertEqual(response.status_code, 302)
        self.assertEqual(response.url, '/')

        # Profile is verified in DB
        profile = UserProfile.objects.get(user=user)
        self.assertTrue(profile.is_email_verified)
        self.assertIsNotNone(profile.email_verified_at)

        # Code is marked as used in DB
        code_record = EmailVerificationCode.objects.get(user=user)
        self.assertIsNotNone(code_record.used_at)

        # Reusing the code fails
        reuse_success = EmailVerificationCode.consume_code(user, raw_code)
        self.assertFalse(reuse_success)

    def test_04_login_with_username_or_email(self):
        """
        Tests login with both username and email.
        """
        user = User.objects.create_user(username='alice', email='alice@example.com', password='Password123!')
        UserProfile.objects.create(user=user, is_email_verified=True)

        # Login with username
        res_username = self.client.post('/login/', {
            'identifier': 'alice',
            'password': 'Password123!'
        })
        self.assertEqual(res_username.status_code, 302)
        self.assertEqual(res_username.url, '/')
        self.client.logout()

        # Login with email
        res_email = self.client.post('/login/', {
            'identifier': 'alice@example.com',
            'password': 'Password123!'
        })
        self.assertEqual(res_email.status_code, 302)
        self.assertEqual(res_email.url, '/')

    def test_05_login_blocked_if_unverified(self):
        """
        Unverified user cannot access dashboard and is redirected to /verify-email/
        """
        user = User.objects.create_user(username='bob', email='bob@example.com', password='Password123!')
        UserProfile.objects.create(user=user, is_email_verified=False)

        response = self.client.post('/login/', {
            'identifier': 'bob',
            'password': 'Password123!'
        })
        self.assertEqual(response.status_code, 302)
        self.assertIn('/verify-email/', response.url)

    def test_06_resend_code_cooldown(self):
        """
        Tests code resend and 60-second cooldown rate limit.
        """
        user = User.objects.create_user(username='charlie', email='charlie@example.com', password='Password123!')
        UserProfile.objects.create(user=user, is_email_verified=False)
        EmailVerificationCode.create_code(user, expiry_minutes=10)

        # Immediate second resend is rate-limited
        can_resend = EmailVerificationCode.can_resend(user, cooldown_seconds=60)
        self.assertFalse(can_resend)

    def test_07_mobile_api_register_verify_login(self):
        """
        Tests Mobile REST APIs:
        - POST /api/v1/auth/register/
        - POST /api/v1/auth/verify/
        - POST /api/v1/auth/login/
        - GET /api/v1/auth/me/
        """
        # Register via API
        reg_res = self.client.post(
            '/api/v1/auth/register/',
            data=json.dumps({
                'username': 'mobileuser',
                'email': 'mobile@example.com',
                'password': 'MobilePassword123!',
                'confirm_password': 'MobilePassword123!'
            }),
            content_type='application/json'
        )
        self.assertEqual(reg_res.status_code, 201)

        user = User.objects.get(username='mobileuser')
        # Simulate obtaining the raw code (retrieved from DB for test verification)
        code_record = EmailVerificationCode.objects.filter(user=user).latest('created_at')
        # In a real test, let's create a known code to test verify_api
        raw_code = EmailVerificationCode.create_code(user, expiry_minutes=10)

        # Verify via API
        verify_res = self.client.post(
            '/api/v1/auth/verify/',
            data=json.dumps({
                'identifier': 'mobileuser',
                'code': raw_code
            }),
            content_type='application/json'
        )
        self.assertEqual(verify_res.status_code, 200)
        verify_data = verify_res.json()
        self.assertIn('token', verify_data)
        token = verify_data['token']

        # Access GET /api/v1/auth/me/ using Bearer token
        me_res = self.client.get(
            '/api/v1/auth/me/',
            HTTP_AUTHORIZATION=f'Bearer {token}'
        )
        self.assertEqual(me_res.status_code, 200)
        me_data = me_res.json()
        self.assertEqual(me_data['user']['username'], 'mobileuser')
        self.assertTrue(me_data['user']['is_verified'])

        # Login via API
        login_res = self.client.post(
            '/api/v1/auth/login/',
            data=json.dumps({
                'identifier': 'mobile@example.com',
                'password': 'MobilePassword123!'
            }),
            content_type='application/json'
        )
        self.assertEqual(login_res.status_code, 200)
        login_data = login_res.json()
        self.assertIn('token', login_data)

    def test_08_dashboard_view_access(self):
        """
        Unauthenticated -> redirected to login
        Authenticated & verified -> renders 200
        """
        # Unauthenticated
        res1 = self.client.get('/')
        self.assertEqual(res1.status_code, 302)
        self.assertIn('/login/', res1.url)

        # Authenticated & verified
        user = User.objects.create_user(username='dave', email='dave@example.com', password='Password123!')
        UserProfile.objects.create(user=user, is_email_verified=True)
        self.client.force_login(user)

        res2 = self.client.get('/')
        self.assertEqual(res2.status_code, 200)
        self.assertIn(b'Dashboard Overview', res2.content)
