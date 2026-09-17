import logging
from django.contrib.auth import get_user_model
from django.core.mail import send_mail
from django.conf import settings
from .models import UserProfile, EmailVerificationCode, PasswordResetCode, MobileAuthToken

logger = logging.getLogger('accounts.services')
User = get_user_model()


def find_user_by_identifier(identifier: str):
    """
    Finds a user by either username or email (case-insensitive).
    """
    if not identifier:
        return None

    clean_id = identifier.strip()
    # Check email first if it looks like an email
    if '@' in clean_id:
        user = User.objects.filter(email__iexact=clean_id).first()
        if user:
            return user

    # Check username
    user = User.objects.filter(username__iexact=clean_id).first()
    if user:
        return user

    # Fallback search email again
    return User.objects.filter(email__iexact=clean_id).first()


def authenticate_user(identifier: str, password: str):
    """
    Authenticates a user by username or email and password.
    Returns (user, error_code, error_message)
    error_code can be: None, 'INVALID_CREDENTIALS', 'EMAIL_NOT_VERIFIED', 'INACTIVE'
    """
    if not identifier or not password:
        return None, 'INVALID_CREDENTIALS', 'Identifier and password are required.'

    user = find_user_by_identifier(identifier)
    if not user or not user.check_password(password):
        return None, 'INVALID_CREDENTIALS', 'Invalid username/email or password.'

    if not user.is_active:
        return None, 'INACTIVE', 'This account has been disabled.'

    profile = UserProfile.get_or_create_for_user(user)
    if not profile.is_email_verified:
        return user, 'EMAIL_NOT_VERIFIED', 'Your email address is not verified. Please verify your email to continue.'

    return user, None, None


def send_verification_email(user, code: str) -> bool:
    """
    Sends the 6-digit verification code to the user's email.
    """
    subject = "Amar Hishab - Email Verification Code"
    message = (
        f"Hello {user.first_name or user.username},\n\n"
        f"Thank you for registering with Amar Hishab.\n\n"
        f"Your Amar Hishab verification code is: {code}\n\n"
        f"Please enter this 6-digit code on the verification page to activate your account.\n"
        f"This code will expire in 10 minutes and can only be used once.\n\n"
        f"If you did not request this verification code, please disregard this email.\n\n"
        f"— The Amar Hishab Team\n"
        f"https://amar-hisab.onrender.com/"
    )
    from_email = getattr(settings, 'DEFAULT_FROM_EMAIL', 'noreply@amarhishab.app')
    try:
        send_mail(
            subject=subject,
            message=message,
            from_email=from_email,
            recipient_list=[user.email],
            fail_silently=False
        )
        logger.info(f"Verification code sent to {user.email}")
        return True
    except Exception as e:
        logger.error(f"Failed to send verification email to {user.email}: {e}")
        return False


def send_password_reset_email(user, code: str) -> bool:
    """
    Sends the 6-digit password reset code to the user's email.
    """
    subject = "Amar Hishab - Password Reset Code"
    message = (
        f"Hello {user.first_name or user.username},\n\n"
        f"We received a request to reset your Amar Hishab account password.\n\n"
        f"Your 6-digit password reset code is: {code}\n\n"
        f"This code will expire in 15 minutes and can only be used once.\n\n"
        f"If you did not request a password reset, please secure your account immediately.\n\n"
        f"— Amar Hishab Team"
    )
    from_email = getattr(settings, 'DEFAULT_FROM_EMAIL', 'noreply@amarhishab.app')
    try:
        send_mail(
            subject=subject,
            message=message,
            from_email=from_email,
            recipient_list=[user.email],
            fail_silently=False
        )
        logger.info(f"Password reset code sent to {user.email}")
        return True
    except Exception as e:
        logger.error(f"Failed to send password reset email to {user.email}: {e}")
        return False
