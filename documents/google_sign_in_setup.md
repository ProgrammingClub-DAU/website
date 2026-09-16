# Google Sign-In Setup

Written for whoever holds the club's Google Cloud and hosting accounts. Follow it
once, before the Google-only sign-in change is merged.

## Why the order matters

After this change there is exactly one way into the site: a Google account on
`@dau.ac.in`. The email and password form is gone, and so is the registration
page.

That means if the OAuth client is not configured when the change goes live,
**nobody can sign in, including you and including every admin**. There is no
password fallback to rescue the situation with.

So the order is:

1. Do the Google Cloud Console setup below.
2. Set the environment variables on Vercel and Render.
3. Redeploy both.
4. Only then merge and deploy the sign-in change.

## Before you start: check for non-DAU accounts

Any existing account whose email is not `@dau.ac.in` will be locked out, because
the address on the account is what a Google sign-in is matched against.

Run this against the production database first:

```sql
SELECT id, name, email FROM users WHERE email NOT LIKE '%@dau.ac.in';
```

If it returns rows, decide for each one before merging:

- A member who has a university address: update the row to it, and they sign in
  normally.

  ```sql
  UPDATE users SET email = '202401226@dau.ac.in' WHERE id = 7;
  ```

- A test or seed account nobody needs: delete it.
- An admin: fix it first. An admin locked out cannot be recovered from the site.

## Step 1: Create the OAuth client

1. Go to https://console.cloud.google.com and create a project, or pick the
   club's existing one.

2. **APIs & Services > OAuth consent screen**

   - User type: choose **Internal** if this project belongs to the DAU Google
     Workspace. Internal is the better answer where it is available: only
     accounts in the organisation can use the client at all, so the domain rule
     is enforced by Google as well as by our server. If Internal is not offered,
     choose **External**.
   - App name: `Programming Club @ DAU`
   - Support email and developer contact: your club address.
   - Scopes: leave the defaults (`email`, `profile`, `openid`). Do not add any
     others. Everything beyond these three counts as a sensitive scope and drags
     the app into Google's verification review, which takes weeks.

3. **APIs & Services > Credentials > Create credentials > OAuth client ID**

   - Application type: **Web application**
   - Name: `Programming Club website`
   - **Authorised JavaScript origins** - add both:

     ```
     http://localhost:3000
     https://<your-vercel-domain>
     ```

     Origins only. No path, no trailing slash. `https://example.vercel.app/login`
     is wrong; `https://example.vercel.app` is right.

   - **Authorised redirect URIs**: leave empty. The site uses the ID-token flow,
     which never redirects.

4. Copy the **Client ID**. It looks like
   `123456789-abcdefg.apps.googleusercontent.com`.

   Ignore the client secret. This application never uses it, and it must not be
   put in any environment variable here.

## Step 2: Set the environment variables

The same client ID goes in two places and the two must match exactly. A mismatch
refuses every sign-in, with the server logging a rejected token and the browser
showing only a generic failure, so it is worth checking twice.

**Vercel** (Settings > Environment Variables, all environments):

```
NEXT_PUBLIC_GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
```

This one is inlined into the JavaScript bundle at build time, so **Vercel must be
redeployed** after setting it. Setting it without redeploying changes nothing.

**Render** (Environment > Environment Variables):

```
GOOGLE_CLIENT_ID=123456789-abcdefg.apps.googleusercontent.com
GOOGLE_ALLOWED_DOMAIN=dau.ac.in
```

`GOOGLE_ALLOWED_DOMAIN` has a default of `dau.ac.in`, so it is optional. Set it
anyway: it makes the rule visible to whoever reads the dashboard next.

The backend refuses to start if `GOOGLE_CLIENT_ID` is missing. That is on
purpose - a server that boots with sign-in broken is worse than one that does not
boot, because the failure surfaces later and further from the cause.

## Step 3: Check it works

On the deployed site, before merging anything else:

1. Sign in with your own `@dau.ac.in` account. You should land on the site, or on
   the welcome question if your account has no year recorded yet.
2. Sign in with a personal Gmail. You should be refused, with a message naming
   `@dau.ac.in`. If a personal account gets in, stop and re-check
   `GOOGLE_ALLOWED_DOMAIN` on Render.
3. Confirm your admin account still reaches `/admin`.

## What the server checks

Worth knowing, because it is what the club is relying on:

- **Signature**, against Google's published keys. The token has to be one Google
  actually issued.
- **Audience**, against `GOOGLE_CLIENT_ID`. A token minted for some other site
  cannot be replayed here.
- **Expiry.**
- **`email_verified`.** Google must have confirmed the holder controls the
  address.
- **Address domain**, against `GOOGLE_ALLOWED_DOMAIN`.
- **`hd` hosted-domain claim**, where present. Google sets this only for accounts
  a Workspace actually issued, which is what separates a genuine university
  account from a personal one that merely carries a similar-looking address.

The browser sends a token and nothing else. It never tells the server who it is.

## If you get locked out anyway

Direct database access is the way back in. Promote an account that can sign in:

```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = '202401226@dau.ac.in';
```

The new role takes effect on that member's next request; authorities are read
from the database per request rather than trusted from the token, so there is no
need to sign out and back in.
