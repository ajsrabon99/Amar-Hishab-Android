import urllib.request
import re
import http.cookiejar

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

# 1. Inspect homepage
req = urllib.request.Request("https://amar-hisab.onrender.com/", headers={"User-Agent": "Mozilla/5.0"})
try:
    resp = opener.open(req, timeout=10)
    html = resp.read().decode("utf-8")
    with open("/tmp/home.html", "w") as f:
        f.write(html)
    print("Downloaded home.html, length:", len(html))
except Exception as e:
    print("Error fetching home:", e)

# 2. Inspect accounts/google/login/
req = urllib.request.Request("https://amar-hisab.onrender.com/accounts/google/login/", headers={"User-Agent": "Mozilla/5.0"})
try:
    resp = opener.open(req, timeout=10)
    html = resp.read().decode("utf-8")
    with open("/tmp/google_login.html", "w") as f:
        f.write(html)
    print("Downloaded google_login.html, length:", len(html))
except Exception as e:
    print("Error fetching google_login:", e)

# 3. Inspect accounts/google/login/token/ 401 error body
try:
    req = urllib.request.Request("https://amar-hisab.onrender.com/accounts/google/login/token/", headers={"User-Agent": "Mozilla/5.0"})
    resp = opener.open(req, timeout=10)
    print("token endpoint returned:", resp.status)
except urllib.error.HTTPError as e:
    body = e.read().decode("utf-8", errors="replace")
    with open("/tmp/google_login_token_err.html", "w") as f:
        f.write(body)
    print("token endpoint returned HTTPError:", e.code, "length:", len(body))
except Exception as e:
    print("Error:", e)
