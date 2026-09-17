import json
import logging
import re
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods
from django.contrib.auth import get_user_model
from accounts.models import (
    UserProfile,
    EmailVerificationCode,
    PasswordResetCode,
    MobileAuthToken
)
from accounts.services import (
    find_user_by_identifier,
    authenticate_user,
    send_verification_email,
    send_password_reset_email
)

logger = logging.getLogger('api.views_auth')
User = get_user_model()
EMAIL_REGEX = re.compile(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$')


def extract_bearer_token(request):
    """
    Extracts Bearer token from the Authorization header.
    """
    auth_header = request.headers.get('Authorization') or request.META.get('HTTP_AUTHORIZATION', '')
    if auth_header.startswith('Bearer '):
        return auth_header[7:].strip()
    return None


def get_authenticated_mobile_user(request):
    """
    Returns the User authenticated via the Bearer token, or None.
    """
    token = extract_bearer_token(request)
    if not token:
        return None, None
    user = MobileAuthToken.validate_token(token)
    return user, token


def parse_json_body(request):
    try:
        return json.loads(request.body.decode('utf-8'))
    except Exception:
        return {}


@csrf_exempt
@require_http_methods(["POST"])
def login_api(request):
    """
    POST /api/v1/auth/login/
    Mobile first-party login with username/email and password.
    Issues cryptographically random mobile token (SHA-256 hashed in DB).
    """
    data = parse_json_body(request)
    identifier = data.get('identifier') or data.get('username') or data.get('email') or ''
    password = data.get('password') or ''

    identifier = identifier.strip()
    if not identifier or not password:
        return JsonResponse({
            'status': 'error',
            'error': 'INVALID_REQUEST',
            'message': 'Username/email and password are required.'
        }, status=400)

    user, error_code, error_msg = authenticate_user(identifier, password)

    if error_code == 'EMAIL_NOT_VERIFIED':
        return JsonResponse({
            'status': 'error',
            'error': 'EMAIL_NOT_VERIFIED',
            'message': 'Email is not verified. Please verify your email before logging in.'
        }, status=403)

    if user is None:
        return JsonResponse({
            'status': 'error',
            'error': 'INVALID_CREDENTIALS',
            'message': error_msg or 'Invalid username/email or password.'
        }, status=401)

    # Issue mobile token
    raw_token, expires_at = MobileAuthToken.create_token(user, validity_days=90)
    profile = UserProfile.get_or_create_for_user(user)

    return JsonResponse({
        'status': 'success',
        'token': raw_token,
        'key': raw_token,  # compatibility with existing DTO
        'expires_at': expires_at.isoformat() if expires_at else None,
        'user': {
            'id': str(user.id),
            'username': user.username,
            'email': user.email,
            'first_name': user.first_name,
            'last_name': user.last_name,
            'is_verified': profile.is_email_verified
        },
        'message': 'Login successful'
    }, status=200)


@csrf_exempt
@require_http_methods(["POST"])
def logout_api(request):
    """
    POST /api/v1/auth/logout/
    Revokes the mobile bearer token on the server.
    """
    token = extract_bearer_token(request)
    if token:
        MobileAuthToken.revoke_token(token)

    return JsonResponse({
        'status': 'success',
        'message': 'Logged out successfully'
    }, status=200)


@csrf_exempt
@require_http_methods(["GET"])
def me_api(request):
    """
    GET /api/v1/auth/me/
    Returns authenticated user information.
    """
    user, _ = get_authenticated_mobile_user(request)
    if not user:
        return JsonResponse({
            'status': 'error',
            'error': 'UNAUTHORIZED',
            'message': 'Invalid or expired authentication token.'
        }, status=401)

    profile = UserProfile.get_or_create_for_user(user)

    return JsonResponse({
        'status': 'success',
        'user': {
            'id': str(user.id),
            'username': user.username,
            'email': user.email,
            'first_name': user.first_name,
            'last_name': user.last_name,
            'is_verified': profile.is_email_verified
        }
    }, status=200)


@csrf_exempt
@require_http_methods(["POST"])
def register_api(request):
    """
    POST /api/v1/auth/register/
    Registers a new account and sends 6-digit email verification code.
    """
    data = parse_json_body(request)
    username = (data.get('username') or '').strip()
    email = (data.get('email') or '').strip().lower()
    password = data.get('password') or ''
    confirm_password = data.get('confirm_password') or password

    if not username or not email or not password:
        return JsonResponse({
            'status': 'error',
            'error': 'INVALID_REQUEST',
            'message': 'Username, email, and password are required.'
        }, status=400)

    if len(username) < 3 or not re.match(r'^[a-zA-Z0-9_.]+$', username):
        return JsonResponse({
            'status': 'error',
            'error': 'INVALID_USERNAME',
            'message': 'Username must be at least 3 characters and contain only letters, numbers, dots, and underscores.'
        }, status=400)

    if not EMAIL_REGEX.match(email):
        return JsonResponse({
            'status': 'error',
            'error': 'INVALID_EMAIL',
            'message': 'Invalid email address format.'
        }, status=400)

    if password != confirm_password:
        return JsonResponse({
            'status': 'error',
            'error': 'PASSWORD_MISMATCH',
            'message': 'Passwords do not match.'
        }, status=400)

    if len(password) < 8:
        return JsonResponse({
            'status': 'error',
            'error': 'WEAK_PASSWORD',
            'message': 'Password must be at least 8 characters long.'
        }, status=400)

    if User.objects.filter(username__iexact=username).exists():
        return JsonResponse({
            'status': 'error',
            'error': 'USERNAME_TAKEN',
            'message': 'Username is already taken.'
        }, status=409)

    if User.objects.filter(email__iexact=email).exists():
        return JsonResponse({
            'status': 'error',
            'error': 'EMAIL_TAKEN',
            'message': 'An account with this email already exists.'
        }, status=409)

    user = User.objects.create_user(username=username, email=email, password=password)
    UserProfile.objects.create(user=user, is_email_verified=False)

    code = EmailVerificationCode.create_code(user, expiry_minutes=10)
    send_verification_email(user, code)

    return JsonResponse({
        'status': 'success',
        'message': f'Verification code sent to {email}.',
        'email': email,
        'user': {
            'id': str(user.id),
            'username': user.username,
            'email': user.email,
            'is_verified': False
        }
    }, status=201)


@csrf_exempt
@require_http_methods(["POST"])
def verify_api(request):
    """
    POST /api/v1/auth/verify/
    Verifies email using 6-digit code.
    """
    data = parse_json_body(request)
    identifier = (data.get('identifier') or data.get('email') or data.get('username') or '').strip()
    code = (data.get('code') or '').strip()

    if not identifier or not code:
        return JsonResponse({
            'status': 'error',
            'message': 'Identifier and verification code are required.'
        }, status=400)

    user = find_user_by_identifier(identifier)
    if not user:
        return JsonResponse({
            'status': 'error',
            'message': 'Account not found.'
        }, status=404)

    success = EmailVerificationCode.consume_code(user, code)
    if not success:
        return JsonResponse({
            'status': 'error',
            'message': 'Invalid or expired verification code.'
        }, status=400)

    # Issue mobile token so the mobile app can authenticate immediately
    raw_token, expires_at = MobileAuthToken.create_token(user, validity_days=90)
    profile = UserProfile.get_or_create_for_user(user)

    return JsonResponse({
        'status': 'success',
        'verified': True,
        'authenticated': True,
        'token': raw_token,
        'key': raw_token,
        'expires_at': expires_at.isoformat() if expires_at else None,
        'user': {
            'id': str(user.id),
            'username': user.username,
            'email': user.email,
            'first_name': user.first_name,
            'last_name': user.last_name,
            'is_verified': profile.is_email_verified
        },
        'message': 'Email verified successfully!'
    }, status=200)


@csrf_exempt
@require_http_methods(["POST"])
def resend_code_api(request):
    """
    POST /api/v1/auth/resend-code/
    Resends 6-digit verification code with 60s rate limit.
    """
    data = parse_json_body(request)
    identifier = (data.get('identifier') or data.get('email') or data.get('username') or '').strip()

    if not identifier:
        return JsonResponse({
            'status': 'error',
            'message': 'Identifier is required.'
        }, status=400)

    user = find_user_by_identifier(identifier)
    if not user:
        return JsonResponse({
            'status': 'error',
            'message': 'Account not found.'
        }, status=404)

    if not EmailVerificationCode.can_resend(user, cooldown_seconds=60):
        return JsonResponse({
            'status': 'error',
            'message': 'Please wait 60 seconds before requesting a new code.'
        }, status=429)

    code = EmailVerificationCode.create_code(user, expiry_minutes=10)
    send_verification_email(user, code)

    return JsonResponse({
        'status': 'success',
        'message': f'Verification code resent to {user.email}.'
    }, status=200)


@csrf_exempt
@require_http_methods(["POST"])
def password_change_api(request):
    """
    POST /api/v1/auth/password/change/
    Requires Bearer token. Changes password.
    """
    user, _ = get_authenticated_mobile_user(request)
    if not user:
        return JsonResponse({
            'status': 'error',
            'message': 'Unauthorized.'
        }, status=401)

    data = parse_json_body(request)
    old_password = data.get('old_password') or ''
    new_password = data.get('new_password') or ''

    if not old_password or not new_password:
        return JsonResponse({
            'status': 'error',
            'message': 'Old password and new password are required.'
        }, status=400)

    if not user.check_password(old_password):
        return JsonResponse({
            'status': 'error',
            'message': 'Current password is incorrect.'
        }, status=400)

    if len(new_password) < 8:
        return JsonResponse({
            'status': 'error',
            'message': 'New password must be at least 8 characters long.'
        }, status=400)

    user.set_password(new_password)
    user.save(update_fields=['password'])

    return JsonResponse({
        'status': 'success',
        'message': 'Password changed successfully.'
    }, status=200)


@csrf_exempt
@require_http_methods(["POST"])
def password_reset_api(request):
    """
    POST /api/v1/auth/password/reset/
    Step 1: {"email": "..."} -> sends 6-digit code.
    Step 2: {"email": "...", "code": "...", "new_password": "..."} -> resets password.
    """
    data = parse_json_body(request)
    email = (data.get('email') or '').strip().lower()
    code = (data.get('code') or '').strip()
    new_password = data.get('new_password') or ''

    if not email:
        return JsonResponse({
            'status': 'error',
            'message': 'Email is required.'
        }, status=400)

    user = User.objects.filter(email__iexact=email).first()

    # Step 2: Confirmation
    if code and new_password:
        if not user:
            return JsonResponse({
                'status': 'error',
                'message': 'Invalid or expired reset code.'
            }, status=400)

        if len(new_password) < 8:
            return JsonResponse({
                'status': 'error',
                'message': 'Password must be at least 8 characters long.'
            }, status=400)

        success = PasswordResetCode.consume_code(user, code, new_password)
        if success:
            return JsonResponse({
                'status': 'success',
                'message': 'Password has been reset successfully.'
            }, status=200)
        else:
            return JsonResponse({
                'status': 'error',
                'message': 'Invalid or expired reset code.'
            }, status=400)

    # Step 1: Request code
    if user:
        profile = UserProfile.get_or_create_for_user(user)
        if profile.is_email_verified and PasswordResetCode.can_resend(user, cooldown_seconds=60):
            reset_code = PasswordResetCode.create_code(user, expiry_minutes=15)
            send_password_reset_email(user, reset_code)

    return JsonResponse({
        'status': 'success',
        'message': 'If an account with that email exists, a password reset code was sent.'
    }, status=200)
