import re
from django.shortcuts import render, redirect
from django.contrib.auth import login, logout, get_user_model
from django.contrib import messages
from django.utils.http import url_has_allowed_host_and_scheme
from django.views.decorators.http import require_http_methods
from .models import UserProfile, EmailVerificationCode, PasswordResetCode
from .services import (
    find_user_by_identifier,
    authenticate_user,
    send_verification_email,
    send_password_reset_email,
)

User = get_user_model()
EMAIL_REGEX = re.compile(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$')


@require_http_methods(["GET"])
def dashboard_view(request):
    """
    Amar Hishab Main Financial Dashboard.
    Requires authenticated user with verified email.
    """
    if not request.user.is_authenticated:
        return redirect('/login/?next=/')

    profile = UserProfile.get_or_create_for_user(request.user)
    if not profile.is_email_verified:
        messages.warning(request, "Please verify your email first.")
        return redirect(f'/verify-email/?user={request.user.email}')

    context = {
        'user': request.user,
        'profile': profile,
        'total_balance': "45,250.00",
        'monthly_income': "65,000.00",
        'monthly_expense': "19,750.00",
        'savings_rate': "69.6%",
        'accounts': [
            {'name': 'Cash in Hand', 'type': 'Cash', 'icon': '💵', 'balance': '12,500.00', 'color': '#10b981'},
            {'name': 'City Bank Account', 'type': 'Bank', 'icon': '🏦', 'balance': '25,750.00', 'color': '#3b82f6'},
            {'name': 'bKash Wallet', 'type': 'Mobile Wallet', 'icon': '📱', 'balance': '5,200.00', 'color': '#e11d48'},
            {'name': 'Nagad Wallet', 'type': 'Mobile Wallet', 'icon': '💳', 'balance': '1,800.00', 'color': '#ea580c'},
        ],
        'recent_transactions': [
            {'date': 'Today', 'title': 'Office Salary', 'category': 'Salary', 'type': 'income', 'account': 'City Bank', 'amount': '+55,000.00'},
            {'date': 'Today', 'title': 'Grocery Shopping', 'category': 'Groceries', 'type': 'expense', 'account': 'bKash', 'amount': '-3,450.00'},
            {'date': 'Yesterday', 'title': 'Internet Bill', 'category': 'Utilities', 'type': 'expense', 'account': 'Nagad', 'amount': '-1,200.00'},
            {'date': '12 Sep', 'title': 'Freelance Work', 'category': 'Freelance', 'type': 'income', 'account': 'City Bank', 'amount': '+10,000.00'},
            {'date': '10 Sep', 'title': 'Family Dinner', 'category': 'Food & Dining', 'type': 'expense', 'account': 'Cash', 'amount': '-2,100.00'},
        ]
    }
    return render(request, 'dashboard.html', context)


@require_http_methods(["GET", "POST"])
def login_view(request):
    """
    Website Login: Username or Email + Password.
    """
    if request.user.is_authenticated:
        profile = UserProfile.get_or_create_for_user(request.user)
        if profile.is_email_verified:
            return redirect('/')
        return redirect(f'/verify-email/?user={request.user.email}')

    next_url = request.GET.get('next', '')
    identifier_val = ''

    if request.method == 'POST':
        identifier = request.POST.get('identifier', '').strip()
        password = request.POST.get('password', '')
        identifier_val = identifier

        if not identifier or not password:
            messages.error(request, "Please enter both username/email and password.")
            return render(request, 'accounts/login.html', {
                'identifier': identifier_val,
                'next': next_url
            })

        user, error_code, error_msg = authenticate_user(identifier, password)

        if error_code == 'EMAIL_NOT_VERIFIED':
            messages.warning(
                request,
                "Please verify your email first. A verification code is required."
            )
            return redirect(f'/verify-email/?user={identifier}')

        if user is None:
            messages.error(request, error_msg or "Invalid username/email or password.")
            return render(request, 'accounts/login.html', {
                'identifier': identifier_val,
                'next': next_url
            })

        # Successful login
        login(request, user)
        messages.success(request, f"Welcome back, {user.username}!")

        if next_url and url_has_allowed_host_and_scheme(next_url, allowed_hosts={request.get_host()}):
            return redirect(next_url)
        return redirect('/')

    return render(request, 'accounts/login.html', {
        'identifier': identifier_val,
        'next': next_url
    })


@require_http_methods(["GET", "POST"])
def register_view(request):
    """
    Website Registration: Username, Email, Password, Confirm Password.
    Saves newly registered user to database and dispatches 6-digit email code.
    Redirects to verification page.
    """
    if request.user.is_authenticated:
        profile = UserProfile.get_or_create_for_user(request.user)
        if profile.is_email_verified:
            return redirect('/')
        return redirect(f'/verify-email/?user={request.user.email}')

    if request.method == 'POST':
        username = request.POST.get('username', '').strip()
        email = request.POST.get('email', '').strip().lower()
        password = request.POST.get('password', '')
        confirm_password = request.POST.get('confirm_password', '')

        context = {
            'username': username,
            'email': email
        }

        # Validation
        if not username or not email or not password or not confirm_password:
            messages.error(request, "All fields are required.")
            return render(request, 'accounts/register.html', context)

        if len(username) < 3:
            messages.error(request, "Username must be at least 3 characters long.")
            return render(request, 'accounts/register.html', context)

        if not re.match(r'^[a-zA-Z0-9_.]+$', username):
            messages.error(request, "Username can only contain letters, numbers, dots, and underscores.")
            return render(request, 'accounts/register.html', context)

        if not EMAIL_REGEX.match(email):
            messages.error(request, "Please enter a valid email address.")
            return render(request, 'accounts/register.html', context)

        if password != confirm_password:
            messages.error(request, "Passwords do not match.")
            return render(request, 'accounts/register.html', context)

        if len(password) < 8:
            messages.error(request, "Password must be at least 8 characters long.")
            return render(request, 'accounts/register.html', context)

        if User.objects.filter(username__iexact=username).exists():
            messages.error(request, "This username is already taken. Please choose another.")
            return render(request, 'accounts/register.html', context)

        if User.objects.filter(email__iexact=email).exists():
            messages.error(request, "An account with this email already exists.")
            return render(request, 'accounts/register.html', context)

        # Create user in database
        user = User.objects.create_user(
            username=username,
            email=email,
            password=password
        )
        UserProfile.objects.create(
            user=user,
            is_email_verified=False
        )

        # Generate and dispatch 6-digit verification code
        code = EmailVerificationCode.create_code(user, expiry_minutes=10)
        send_verification_email(user, code)

        messages.success(
            request,
            f"Registration successful! We sent a 6-digit verification code to {email}."
        )
        return redirect(f'/verify-email/?user={email}')

    return render(request, 'accounts/register.html')


@require_http_methods(["GET", "POST"])
def verify_email_view(request):
    """
    Website Email Verification: Enter 6-digit verification code.
    Upon successful code verification:
      - marks account as verified
      - marks code as used
      - creates authenticated Django session automatically
      - redirects directly to Amar Hishab Dashboard
    """
    user_param = (
        request.GET.get('user', '') or
        request.GET.get('identifier', '') or
        request.POST.get('identifier', '')
    ).strip()

    if request.method == 'POST':
        action = request.POST.get('action', 'verify')
        identifier = request.POST.get('identifier', '').strip() or user_param

        user = find_user_by_identifier(identifier)
        if not user:
            messages.error(request, "No account found matching that username or email.")
            return render(request, 'accounts/verify_email.html', {'identifier': identifier})

        if action == 'resend':
            if not EmailVerificationCode.can_resend(user, cooldown_seconds=60):
                messages.warning(request, "Please wait at least 60 seconds before requesting a new code.")
                return render(request, 'accounts/verify_email.html', {'identifier': identifier})

            code = EmailVerificationCode.create_code(user, expiry_minutes=10)
            send_verification_email(user, code)
            messages.success(request, f"A new verification code was sent to {user.email}.")
            return render(request, 'accounts/verify_email.html', {'identifier': identifier})

        # action == 'verify'
        code = request.POST.get('code', '').strip()
        if not code:
            messages.error(request, "Please enter the 6-digit verification code.")
            return render(request, 'accounts/verify_email.html', {'identifier': identifier})

        success = EmailVerificationCode.consume_code(user, code)
        if success:
            # Automatically authenticate user and create normal Django session
            login(request, user)
            messages.success(request, "Email verified successfully! Welcome to Amar Hishab.")
            return redirect('/')
        else:
            messages.error(request, "Invalid or expired verification code. Please check the code and try again.")
            return render(request, 'accounts/verify_email.html', {
                'identifier': identifier,
                'code': code
            })

    return render(request, 'accounts/verify_email.html', {'identifier': user_param})


@require_http_methods(["GET", "POST"])
def resend_code_view(request):
    """
    Dedicated endpoint for POST /resend-code/ (and /accounts/resend-code/).
    Invalidates previous code, generates a new 6-digit code, and sends by email.
    """
    identifier = (
        request.POST.get('identifier', '') or
        request.POST.get('email', '') or
        request.GET.get('user', '') or
        request.GET.get('identifier', '')
    ).strip()

    if not identifier:
        messages.error(request, "Please provide your username or email address.")
        return redirect('/verify-email/')

    user = find_user_by_identifier(identifier)
    if not user:
        messages.error(request, "No account found matching that username or email.")
        return redirect('/verify-email/')

    if not EmailVerificationCode.can_resend(user, cooldown_seconds=60):
        messages.warning(request, "Please wait at least 60 seconds before requesting a new verification code.")
        return redirect(f'/verify-email/?user={identifier}')

    code = EmailVerificationCode.create_code(user, expiry_minutes=10)
    send_verification_email(user, code)
    messages.success(request, f"A new verification code was sent to {user.email}.")
    return redirect(f'/verify-email/?user={identifier}')


@require_http_methods(["GET", "POST"])
def password_reset_request_view(request):
    """
    Password Reset Request: User enters email.
    Sends 6-digit code without exposing if user exists.
    """
    if request.method == 'POST':
        email = request.POST.get('email', '').strip().lower()

        if email:
            user = User.objects.filter(email__iexact=email).first()
            if user:
                profile = UserProfile.get_or_create_for_user(user)
                if profile.is_email_verified:
                    if PasswordResetCode.can_resend(user, cooldown_seconds=60):
                        code = PasswordResetCode.create_code(user, expiry_minutes=10)
                        send_password_reset_email(user, code)

        messages.info(
            request,
            "If an account with that email exists, we have sent a 6-digit password reset code."
        )
        return redirect(f'/password-reset/confirm/?email={email}')

    return render(request, 'accounts/password_reset.html')


@require_http_methods(["GET", "POST"])
def password_reset_confirm_view(request):
    """
    Password Reset Confirmation: Email, Code, New Password, Confirm Password.
    """
    email_param = request.GET.get('email', '') or request.POST.get('email', '')
    email_param = email_param.strip().lower()

    if request.method == 'POST':
        email = request.POST.get('email', '').strip().lower()
        code = request.POST.get('code', '').strip()
        new_password = request.POST.get('new_password', '')
        confirm_password = request.POST.get('confirm_password', '')

        context = {'email': email, 'code': code}

        if not email or not code or not new_password or not confirm_password:
            messages.error(request, "All fields are required.")
            return render(request, 'accounts/password_reset_confirm.html', context)

        if new_password != confirm_password:
            messages.error(request, "Passwords do not match.")
            return render(request, 'accounts/password_reset_confirm.html', context)

        if len(new_password) < 8:
            messages.error(request, "Password must be at least 8 characters long.")
            return render(request, 'accounts/password_reset_confirm.html', context)

        user = User.objects.filter(email__iexact=email).first()
        if not user:
            messages.error(request, "Invalid or expired reset code.")
            return render(request, 'accounts/password_reset_confirm.html', context)

        success = PasswordResetCode.consume_code(user, code, new_password)
        if success:
            messages.success(request, "Password has been reset successfully! Please sign in with your new password.")
            return redirect('/login/')
        else:
            messages.error(request, "Invalid or expired reset code. Please request a new one.")
            return render(request, 'accounts/password_reset_confirm.html', context)

    return render(request, 'accounts/password_reset_confirm.html', {'email': email_param})


@require_http_methods(["GET", "POST"])
def logout_view(request):
    """
    Website Logout.
    """
    logout(request)
    messages.info(request, "You have been signed out.")
    return redirect('/login/')
