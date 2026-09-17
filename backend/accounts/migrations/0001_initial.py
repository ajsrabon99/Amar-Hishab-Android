import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='UserProfile',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('is_email_verified', models.BooleanField(default=False)),
                ('email_verified_at', models.DateTimeField(blank=True, default=None, null=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('updated_at', models.DateTimeField(auto_now=True)),
                ('user', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name='profile', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'db_table': 'auth_user_profile',
            },
        ),
        migrations.CreateModel(
            name='EmailVerificationCode',
            fields=[
                ('id', models.BigAutoField(primary_key=True, serialize=False)),
                ('code_hash', models.CharField(db_index=True, max_length=64)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('expires_at', models.DateTimeField(db_index=True)),
                ('used_at', models.DateTimeField(blank=True, default=None, null=True)),
                ('user', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='email_verification_codes', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'db_table': 'auth_email_verification_code',
                'indexes': [models.Index(fields=['user', 'code_hash', 'expires_at', 'used_at'], name='auth_email__user_id_code_idx')],
            },
        ),
        migrations.CreateModel(
            name='PasswordResetCode',
            fields=[
                ('id', models.BigAutoField(primary_key=True, serialize=False)),
                ('code_hash', models.CharField(db_index=True, max_length=64)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('expires_at', models.DateTimeField(db_index=True)),
                ('used_at', models.DateTimeField(blank=True, default=None, null=True)),
                ('user', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='password_reset_codes', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'db_table': 'auth_password_reset_code',
                'indexes': [models.Index(fields=['user', 'code_hash', 'expires_at', 'used_at'], name='auth_reset__user_id_code_idx')],
            },
        ),
        migrations.CreateModel(
            name='MobileAuthToken',
            fields=[
                ('id', models.BigAutoField(primary_key=True, serialize=False)),
                ('token_hash', models.CharField(db_index=True, max_length=64, unique=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('expires_at', models.DateTimeField(blank=True, null=True)),
                ('revoked_at', models.DateTimeField(blank=True, default=None, null=True)),
                ('last_used_at', models.DateTimeField(blank=True, default=None, null=True)),
                ('user', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name='mobile_auth_tokens', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'db_table': 'auth_mobile_token',
            },
        ),
    ]
