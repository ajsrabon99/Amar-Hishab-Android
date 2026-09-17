from django.urls import path
from .views_auth import (
    login_api,
    logout_api,
    me_api,
    register_api,
    verify_api,
    resend_code_api,
    password_change_api,
    password_reset_api,
)

urlpatterns = [
    path('auth/login/', login_api, name='api_auth_login'),
    path('auth/logout/', logout_api, name='api_auth_logout'),
    path('auth/me/', me_api, name='api_auth_me'),
    path('auth/register/', register_api, name='api_auth_register'),
    path('auth/verify/', verify_api, name='api_auth_verify'),
    path('auth/verify-email/', verify_api, name='api_auth_verify_email'),
    path('auth/resend-code/', resend_code_api, name='api_auth_resend_code'),
    path('auth/password/change/', password_change_api, name='api_auth_password_change'),
    path('auth/password/reset/', password_reset_api, name='api_auth_password_reset'),
    path('auth/password-reset/', password_reset_api, name='api_auth_password_reset_alt'),
]
