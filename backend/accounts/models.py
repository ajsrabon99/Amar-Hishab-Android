import hashlib
import secrets
from datetime import timedelta
from django.conf import settings
from django.db import models, transaction
from django.utils import timezone


class UserProfile(models.Model):
    """
    Profile tracking verification status and metadata for a user.
    """
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='profile'
    )
    is_email_verified = models.BooleanField(default=False)
    email_verified_at = models.DateTimeField(null=True, blank=True, default=None)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'auth_user_profile'

    def __str__(self):
        return f"Profile(user={self.user_id}, verified={self.is_email_verified})"

    @classmethod
    def get_or_create_for_user(cls, user):
        profile, _ = cls.objects.get_or_create(user=user)
        return profile


class EmailVerificationCode(models.Model):
    """
    Cryptographically secure, single-use, expiring email verification code.
    Only the SHA-256 hash is stored in the database.
    """
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='email_verification_codes'
    )
    code_hash = models.CharField(max_length=64, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField(db_index=True)
    used_at = models.DateTimeField(null=True, blank=True, default=None)

    class Meta:
        db_table = 'auth_email_verification_code'
        indexes = [
            models.Index(fields=['user', 'code_hash', 'expires_at', 'used_at']),
        ]

    def __str__(self):
        return f"EmailVerificationCode(user={self.user_id}, used={self.used_at is not None})"

    @classmethod
    def create_code(cls, user, expiry_minutes=10) -> str:
        """
        Generates a 6-digit random code, invalidates previous codes,
        saves the SHA-256 hash, and returns the raw code.
        """
        now = timezone.now()
        # Invalidate previous unused codes for this user
        cls.objects.filter(user=user, used_at__isnull=True).update(expires_at=now)

        raw_code = f"{secrets.randbelow(900000) + 100000}"
        code_hash = hashlib.sha256(raw_code.encode('utf-8')).hexdigest()
        expires_at = now + timedelta(minutes=expiry_minutes)

        cls.objects.create(
            user=user,
            code_hash=code_hash,
            expires_at=expires_at
        )
        return raw_code

    @classmethod
    def can_resend(cls, user, cooldown_seconds=60) -> bool:
        """
        Enforces rate limiting on code resend requests.
        """
        cutoff = timezone.now() - timedelta(seconds=cooldown_seconds)
        recent_code = cls.objects.filter(
            user=user,
            created_at__gte=cutoff
        ).exists()
        return not recent_code

    @classmethod
    def consume_code(cls, user, raw_code: str) -> bool:
        """
        Atomically validates and marks code as used, then marks user email as verified.
        """
        if not raw_code or not user:
            return False

        code_hash = hashlib.sha256(raw_code.strip().encode('utf-8')).hexdigest()
        now = timezone.now()

        with transaction.atomic():
            try:
                record = cls.objects.select_for_update().get(
                    user=user,
                    code_hash=code_hash,
                    expires_at__gt=now,
                    used_at__isnull=True
                )
            except cls.DoesNotExist:
                return False

            record.used_at = now
            record.save(update_fields=['used_at'])

            profile = UserProfile.get_or_create_for_user(user)
            profile.is_email_verified = True
            profile.email_verified_at = now
            profile.save(update_fields=['is_email_verified', 'email_verified_at'])
            return True


class PasswordResetCode(models.Model):
    """
    Cryptographically secure, single-use, expiring password reset code.
    Only the SHA-256 hash is stored in the database.
    """
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='password_reset_codes'
    )
    code_hash = models.CharField(max_length=64, db_index=True)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField(db_index=True)
    used_at = models.DateTimeField(null=True, blank=True, default=None)

    class Meta:
        db_table = 'auth_password_reset_code'
        indexes = [
            models.Index(fields=['user', 'code_hash', 'expires_at', 'used_at']),
        ]

    def __str__(self):
        return f"PasswordResetCode(user={self.user_id}, used={self.used_at is not None})"

    @classmethod
    def create_code(cls, user, expiry_minutes=15) -> str:
        raw_code = f"{secrets.randbelow(900000) + 100000}"
        code_hash = hashlib.sha256(raw_code.encode('utf-8')).hexdigest()
        expires_at = timezone.now() + timedelta(minutes=expiry_minutes)

        cls.objects.create(
            user=user,
            code_hash=code_hash,
            expires_at=expires_at
        )
        return raw_code

    @classmethod
    def can_resend(cls, user, cooldown_seconds=60) -> bool:
        cutoff = timezone.now() - timedelta(seconds=cooldown_seconds)
        return not cls.objects.filter(user=user, created_at__gte=cutoff).exists()

    @classmethod
    def consume_code(cls, user, raw_code: str, new_password: str) -> bool:
        if not raw_code or not user or not new_password:
            return False

        code_hash = hashlib.sha256(raw_code.strip().encode('utf-8')).hexdigest()
        now = timezone.now()

        with transaction.atomic():
            try:
                record = cls.objects.select_for_update().get(
                    user=user,
                    code_hash=code_hash,
                    expires_at__gt=now,
                    used_at__isnull=True
                )
            except cls.DoesNotExist:
                return False

            record.used_at = now
            record.save(update_fields=['used_at'])

            user.set_password(new_password)
            user.save(update_fields=['password'])
            return True


class MobileAuthToken(models.Model):
    """
    Opaque bearer token for mobile API authentication.
    Only the SHA-256 hash is persisted in the database; raw token is returned once.
    """
    id = models.BigAutoField(primary_key=True)
    token_hash = models.CharField(max_length=64, unique=True, db_index=True)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='mobile_auth_tokens'
    )
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField(null=True, blank=True)
    revoked_at = models.DateTimeField(null=True, blank=True, default=None)
    last_used_at = models.DateTimeField(null=True, blank=True, default=None)

    class Meta:
        db_table = 'auth_mobile_token'

    def __str__(self):
        return f"MobileAuthToken(user={self.user_id}, active={self.is_active})"

    @property
    def is_active(self) -> bool:
        if self.revoked_at is not None:
            return False
        if self.expires_at and self.expires_at <= timezone.now():
            return False
        return True

    @classmethod
    def create_token(cls, user, validity_days=90):
        """
        Generates a 48-byte cryptographically random token, hashes with SHA-256,
        and saves. Returns (raw_token, expires_at).
        """
        raw_token = secrets.token_urlsafe(48)
        token_hash = hashlib.sha256(raw_token.encode('utf-8')).hexdigest()
        expires_at = timezone.now() + timedelta(days=validity_days)

        cls.objects.create(
            token_hash=token_hash,
            user=user,
            expires_at=expires_at
        )
        return raw_token, expires_at

    @classmethod
    def validate_token(cls, raw_token: str):
        """
        Validates the bearer token, updates last_used_at, and returns the User.
        """
        if not raw_token:
            return None

        token_hash = hashlib.sha256(raw_token.strip().encode('utf-8')).hexdigest()
        now = timezone.now()

        try:
            record = cls.objects.select_related('user').get(
                token_hash=token_hash,
                revoked_at__isnull=True
            )
            if record.expires_at and record.expires_at <= now:
                return None

            record.last_used_at = now
            record.save(update_fields=['last_used_at'])
            return record.user
        except cls.DoesNotExist:
            return None

    @classmethod
    def revoke_token(cls, raw_token: str) -> bool:
        """
        Revokes a bearer token server-side.
        """
        if not raw_token:
            return False

        token_hash = hashlib.sha256(raw_token.strip().encode('utf-8')).hexdigest()
        updated = cls.objects.filter(
            token_hash=token_hash,
            revoked_at__isnull=True
        ).update(revoked_at=timezone.now())
        return updated > 0
