from django.contrib import admin
from django.urls import path, include
from accounts.views_web import (
    dashboard_view,
    login_view,
    register_view,
    verify_email_view,
    resend_code_view,
    password_reset_request_view,
    password_reset_confirm_view,
    logout_view,
)

urlpatterns = [
    path('admin/', admin.site.urls),

    # Primary Root Routes
    path('', dashboard_view, name='dashboard'),
    path('dashboard/', dashboard_view, name='dashboard_alt'),
    path('register/', register_view, name='register'),
    path('signup/', register_view, name='signup'),
    path('verify-email/', verify_email_view, name='verify_email'),
    path('resend-code/', resend_code_view, name='resend_code'),
    path('login/', login_view, name='login'),
    path('signin/', login_view, name='signin'),
    path('logout/', logout_view, name='logout'),
    path('password-reset/', password_reset_request_view, name='password_reset'),
    path('password-reset/confirm/', password_reset_confirm_view, name='password_reset_confirm'),

    # Accounts prefix compatibility routes
    path('accounts/', include('accounts.urls')),

    # REST APIs (support both /api/v1/ and /api/)
    path('api/v1/', include('api.urls')),
    path('api/', include('api.urls')),
]
