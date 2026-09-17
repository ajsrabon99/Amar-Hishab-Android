from django.urls import path
from .views_web import (
    dashboard_view,
    login_view,
    register_view,
    verify_email_view,
    resend_code_view,
    password_reset_request_view,
    password_reset_confirm_view,
    logout_view
)

urlpatterns = [
    path('login/', login_view, name='account_login'),
    path('signin/', login_view, name='account_signin'),
    path('signup/', register_view, name='account_signup'),
    path('register/', register_view, name='account_register'),
    path('verify-email/', verify_email_view, name='account_verify_email'),
    path('resend-code/', resend_code_view, name='account_resend_code'),
    path('password/reset/', password_reset_request_view, name='account_password_reset'),
    path('password/reset/confirm/', password_reset_confirm_view, name='account_password_reset_confirm'),
    path('password-reset/', password_reset_request_view, name='account_password_reset_alt'),
    path('password-reset/confirm/', password_reset_confirm_view, name='account_password_reset_confirm_alt'),
    path('logout/', logout_view, name='account_logout'),
    path('dashboard/', dashboard_view, name='account_dashboard'),
]
