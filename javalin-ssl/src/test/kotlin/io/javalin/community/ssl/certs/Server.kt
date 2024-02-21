package io.javalin.community.ssl.certs

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.function.Supplier

object Server {
    const val CERTIFICATE_AS_STRING =
        """-----BEGIN CERTIFICATE-----
MIIDpjCCAo6gAwIBAgIUKK29nJVFCs8SjBqcvxrg7boyem8wDQYJKoZIhvcNAQEL
BQAwQjESMBAGA1UEAwwJbG9jYWxob3N0MQswCQYDVQQGEwJFUzEQMA4GA1UECAwH
R2FsaWNpYTENMAsGA1UEBwwEVmlnbzAgFw0yMjA3MDYxMTQyMDdaGA80MDA1MDMx
MjExNDIwN1owQjESMBAGA1UEAwwJbG9jYWxob3N0MQswCQYDVQQGEwJFUzEQMA4G
A1UECAwHR2FsaWNpYTENMAsGA1UEBwwEVmlnbzCCASIwDQYJKoZIhvcNAQEBBQAD
ggEPADCCAQoCggEBALtW247iPVAuCcQByuqgj8tSzJcwVqCmheT6ld0Xe7DYoLOL
EsjilB/jgG9aBEBfYJ2h74K7SIdqiSDz4rgUuJUzhZnJo5d3n3wT9Wb2AZcsqFce
JK0UNBKe2/1b01dFWtQFW4zHC/JM/Gp0dMTy1Vt1Zf/3SmQjSD/KzgJf4m2O/GOP
3iRFsCSPC4CU3TZCDmI5/qRr4icJCY5s3gJ+RT+edfsvtdkfAO4hK/p+37RrwHax
nyFLoAzYdJMcnDX/+V7Ez2y7jkTkcUk2gKG+3dpio2XqAE9pXcXa4kYk0NL9Vw6L
C2QMefFKHLDqLWx/bfQXpbULFawldETDbuLVe7UCAwEAAaOBkTCBjjAdBgNVHQ4E
FgQUiiPTBoFstcGbb0zYWsM/ZwupRRYwHwYDVR0jBBgwFoAUiiPTBoFstcGbb0zY
WsM/ZwupRRYwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggr
BgEFBQcDAjAMBgNVHRMBAf8EAjAAMA8GA1UdEQQIMAaHBH8AAAEwDQYJKoZIhvcN
AQELBQADggEBAGvqUrtYWZpKBJNYL4UVLnm2+dQl33l8BH7PhU6YvMufThDCVjOw
IJ7ezOReDlCAmytQD7ChKpsJrAOBzKRdrifL0f88psbE83+6Ys/s/1rHMq282p/S
WPRiZDVO8Mw2ra9v9b6cprW5phHJkp7TiIBP82A+v19lt3R+vE4HZ91ZyioNqMzf
Aqvd5gfxHexpilgil0osF0o/8ajSnLiBfWI82Lz/1JB+xUMYW91ahRgt13/54h13
eL70steoAmx55he3pQaaeRZKzI1nLxsrTkjs055jDn0G/yj1L6kY3OeVFg3AhETJ
sg+yATMTef2Qskr4dgzb1LJkC9meaU2TFwk=
-----END CERTIFICATE-----"""
    val CERTIFICATE_INPUT_STREAM_SUPPLIER = Supplier<InputStream> {
        ByteArrayInputStream(
            CERTIFICATE_AS_STRING.toByteArray(
                StandardCharsets.UTF_8
            )
        )
    }
    const val NORWAY_CERTIFICATE_AS_STRING =
        """-----BEGIN CERTIFICATE-----
MIIDrjCCApagAwIBAgIUZ8z5/me7+2mTkDnIYyx1dKjwYMQwDQYJKoZIhvcNAQEL
BQAwRjESMBAGA1UEAwwJbG9jYWxob3N0MQswCQYDVQQGEwJOTzESMBAGA1UECAwJ
SG9yZGFsYW5kMQ8wDQYDVQQHDAZCZXJnZW4wIBcNMjIxMTI2MTIyNjI1WhgPMzAy
MjAzMjkxMjI2MjVaMEYxEjAQBgNVBAMMCWxvY2FsaG9zdDELMAkGA1UEBhMCTk8x
EjAQBgNVBAgMCUhvcmRhbGFuZDEPMA0GA1UEBwwGQmVyZ2VuMIIBIjANBgkqhkiG
9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1bbjuI9UC4JxAHK6qCPy1LMlzBWoKaF5PqV
3Rd7sNigs4sSyOKUH+OAb1oEQF9gnaHvgrtIh2qJIPPiuBS4lTOFmcmjl3effBP1
ZvYBlyyoVx4krRQ0Ep7b/VvTV0Va1AVbjMcL8kz8anR0xPLVW3Vl//dKZCNIP8rO
Al/ibY78Y4/eJEWwJI8LgJTdNkIOYjn+pGviJwkJjmzeAn5FP551+y+12R8A7iEr
+n7ftGvAdrGfIUugDNh0kxycNf/5XsTPbLuORORxSTaAob7d2mKjZeoAT2ldxdri
RiTQ0v1XDosLZAx58UocsOotbH9t9BeltQsVrCV0RMNu4tV7tQIDAQABo4GRMIGO
MB0GA1UdDgQWBBSKI9MGgWy1wZtvTNhawz9nC6lFFjAfBgNVHSMEGDAWgBSKI9MG
gWy1wZtvTNhawz9nC6lFFjAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYB
BQUHAwEGCCsGAQUFBwMCMAwGA1UdEwEB/wQCMAAwDwYDVR0RBAgwBocEfwAAATAN
BgkqhkiG9w0BAQsFAAOCAQEAd+wmmJf/LYXPWhIAf/qBa7HOH1gbzOQ1CMH6qyxm
ueH4MdUr2tewC53M0eYVfEMwOc4NVBmXXKLANkDByjBKGObr4N5cHjyUI1zk0Eew
tkkv8IoHl9pJTewNCdDkgCPADRoQ//gOkqpzxh3uEdhjHAB/KlyylUkxnbAJH9ap
v7V2Ju/yOjXtA0Pl+fdaeYg1Y8TVKLJSUqSR2CFHQgodxMsSG/l2QbpHXVIt50sm
fQxiURVXr/qqP7KubKb2MzRQWVIQemlK3FJgVoY4Mp3zCmSBBq/N2Dioudu/XQQu
tkLrsE6joYC2ST27wJDgYgAY7CmFelbIRZZl94FzR4Jp5A==
-----END CERTIFICATE-----"""
    const val NON_ENCRYPTED_KEY_AS_STRING =
        """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VtuO4j1QLgnE
AcrqoI/LUsyXMFagpoXk+pXdF3uw2KCzixLI4pQf44BvWgRAX2Cdoe+Cu0iHaokg
8+K4FLiVM4WZyaOXd598E/Vm9gGXLKhXHiStFDQSntv9W9NXRVrUBVuMxwvyTPxq
dHTE8tVbdWX/90pkI0g/ys4CX+Jtjvxjj94kRbAkjwuAlN02Qg5iOf6ka+InCQmO
bN4CfkU/nnX7L7XZHwDuISv6ft+0a8B2sZ8hS6AM2HSTHJw1//lexM9su45E5HFJ
NoChvt3aYqNl6gBPaV3F2uJGJNDS/VcOiwtkDHnxShyw6i1sf230F6W1CxWsJXRE
w27i1Xu1AgMBAAECggEAfPI7UZr3BckO3lnLup0ICrXYmmW1AUTPPJ8c4O7Oom55
EAaLqsvjuzkC6kGBYGW8jKX6lpjOkPKvLvk6l0fKrEhGrQFdSKKSDjFJlTgya19v
j1sdXwqAiILHer2JwUUShSJlowkGoL5UA7RURR8oye0M8KFATnVxtIpQyCinXiW/
LkDuqUr8MIbu6V/KcoSOLfJyTWyuwSRPHuFKhv154UAqaTkSPbf2mCTa9hH5Tb4f
Lfzy9o3Ux4ieZceG28De+SmC7uMzbBs1stowOuDmFg3znI/1Br/sQEAXPFngDe3s
soDD2PbLo7/4SPBNgl5vygf7jhvxHPY3DTUXOxLSgQKBgQD4EzKVTx/GpF7Yswma
oixidzSi/KnHJiMjIERF4QPVfDNnggRORNMbPnRhNWSRhS7r+INYbN4yB/vBZO5I
IIqowdJbLjGbmq91equP0zzrP2wCjqtFK6gRElX7acAWY5xTesIT5Fa1Ug++dFLS
MxCZKL6JMZaHJzZVzXugaltMsQKBgQDBUvPSaDnIBrZGdNtAyNMxZyVbp/ObIKW1
TvCDX2hqf+yiTVclbZr5QkwCE3MHErfsKlWU01K9CtzsQh4u9L5tPaeFlvm6iZq6
ktbflNvI+z+qEW3JbROR4WwwbmWFvKRLBA0OQom7tGuNnNyRtkDFxlkFJPcD6Eff
ZEq+ewrQRQKBgQCV7URM6J0TuJN58/qB8jFQ8Spmtr0FFw91UzLv6KYgiAepLvLb
Os07UeuUNGiragqJoo//CQzgv+JvZ0h7Xu9uPnWblbd1i28vWQwGyGuw4Yutn/vy
ugfBCYvdfnQRE/KOoUpaK04cF5RcToEfeK03Y2CEGewXkqNMB/wHXz/+gQKBgE8Y
34WQ+0Mp69375dEl2bL23sQXfYZU3zfFaoZ1vMUGPg1R03wO0j91rp+S0ZdtQy8v
SwCvTcTm8uj/TFYt8NPFTAtOcDKwJkx708p6n0ol8jBlHSQyqrUfJCLUqFkFi7rd
l3HkK3JPKUoxidVcWjgRJU8DhsVkfjOaVzKEKTJ5AoGARBwn7gt2H35urQ6/U3nJ
hFjOVn01F5uV0NvRtRDCsAIUMeA2T4pwALUUIqlA9HmpwYgLeG4bZ+SkhNpy70N/
qcufT1DeM+q3H5zFPANyjcqVaqa6KUnttvi/lhxMdRb6GsA9TzzHzY1P9ovpIOCK
IS639NPzxpI0Ka+v6t+nFEM=
-----END PRIVATE KEY-----
"""
    val NON_ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER = Supplier<InputStream> {
        ByteArrayInputStream(
            NON_ENCRYPTED_KEY_AS_STRING.toByteArray(StandardCharsets.UTF_8)
        )
    }
    const val ENCRYPTED_KEY_AS_STRING =
        """-----BEGIN ENCRYPTED PRIVATE KEY-----
MIIFLTBXBgkqhkiG9w0BBQ0wSjApBgkqhkiG9w0BBQwwHAQIMDP+/JKdUc4CAggA
MAwGCCqGSIb3DQIJBQAwHQYJYIZIAWUDBAEqBBBRfpz0ZTscSvALfUISuNYIBIIE
0I8rVF2h/qQANJ3WvXFcmm7dFqEiQwUm8cDxaDpPd8RRweclTesEj70yg+3xcGLh
rhSFrNSB2wmy/jB6lFcN02KcSU3p6H7aSuLRffbYYAQ3LGU6Ie79NW68x189zB/b
sDi6gWkxHCrGzBydKR4a6ZvF9TMnP743hCw3t3NrO/4xdoZ9+YaxmBaBjt4E1Bns
J2yCHHV5kXXsWOZJvTTWxf+fIEQNe1cjidBxcpvQxreZpOsday2KM8tctom+p9lw
DEF0mhUi/FHZZnmfgr1Cz4+PmspjOTykX+0RWD1wi0kMJwqo6aRHwlEbEE+f83Df
kazqIXOfD0VrzSXTwr1kIzjQI+DK8sKyfg5lfTby1AFy5cvtJxL7cK6As9Cq05XI
i2fX5PWUj1sHplMOI2+qh31R7w6qb0DygXC22ZFNLlFYwP0QKPp9XzZZLIvPI662
9xlF4VgtcS9JV7hztrg6Bbc23l1cSsBXPqreWd39NM/Kggf6J3GV/P0AacYYp0OY
A3Pt9i+RV+HHv8OwfZ+v4RH8hVhtDkPWyBJX581zwF5OQLqjksKa1FNC8qB/VlE4
Ponm33vn1gWtKY962sYoJxDVHbgWwpWP7bSqtO66jiwlTEyQwltd3LbZGcJ4yJNd
eEJUE8vFHyXmEx1KoDHUh2/v9l6pdo59PgBlY8mxl95AuTNds+dtuqZQE6ZNZvDZ
lw7dOp+BATb/3YWCF5O6jQjm11ji9kZxgnPBTSiaegFFIa4OxdQwP2pxbyVk4rGH
/gs6olWtc3hqqQspMJDqT2cJeaE61us7hUya1w1LjivOvofR3Zt2v2Wtxmm+ey3e
/mPBZH1LIRdP9vEuHxKkjjppXVRWL5TdHeN9Ai6jG3/WCp/NgBnjOi9/5GVX7j+T
dUzGBaxwUGt10QZ8Yvo9qT8Cg2tiUD770EzaD09aiRfoAs7YwLsVi35gul+JyNrD
CzqZ8I2NZ6Uo/r9I9Xda9qkoxbS2hNg+53whm5L2fT4SrJ69MOY/tM3mR8q1Ta18
W+dXuFSD+3nAU7Aqug4LlKcOS9/RW18kRtHRVatXZscxITO5dlgmw7zEVzrkwa+q
r1y4YG488XZZ3KCXPJthnmP4nIYERW6hn62P8EKzM8wfxxT39O96QNzMgszor/WA
TG8o0JDRvG5WW/OfVA4Ls8QK6lx3E2cPhyqnvM+HirtP2xL4Gd0SibGIK7QvewSf
9a5TnbQsuoWvTqtzX7PEb9snLxQjaxTLZEbTyimwEyaaZ1Ev72did2EdmA3UEtzq
e+X86mvYOZrAJIWNGIfoMI3QPtxlC2MbDjUcLB5crk90T2dCcIdwpr6cKGdnEqP+
DmkkwTl84MSV2tVQ2qCJPtiwsR8V2xkwqesD1p0G5whR2SxsUQDoG48l1zRkLrA5
PbwUii9Xapp5+R08t+dIt19cRJyewjAKxpWkKHNjtXmBMJpvJ65A4wAAV5vqcTdY
FIrJEMySqFDrodCwkAs9s8FKIWvEnWKkaX2NvjoTWdQEGmKpiEazUsknd4wNX8js
MjjY/VHqWNYR6cF84H+WuFS86S37Vt3nBEpos0vp9n8epNNC+ETcewKMgovLJMnt
na5mQXa7ctzrJ+bqW9B+QLBX6KZk3tRnigYO6Fum0t7I
-----END ENCRYPTED PRIVATE KEY-----
"""
    val ENCRYPTED_KEY_INPUT_STREAM_SUPPLIER = Supplier<InputStream> {
        ByteArrayInputStream(
            ENCRYPTED_KEY_AS_STRING.toByteArray(
                StandardCharsets.UTF_8
            )
        )
    }
    const val GOOGLE_CERTIFICATE_AS_STRING =
        """-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIULxRctoyJitKoVft6Dn6F458+uK8wDQYJKoZIhvcNAQEL
BQAwJzETMBEGA1UEAwwKZ29vZ2xlLmNvbTEQMA4GA1UECgwHSmF2YWxpbjAgFw0y
MjA4MDgwOTE4NDRaGA8yMTIyMDcxNTA5MTg0NFowJzETMBEGA1UEAwwKZ29vZ2xl
LmNvbTEQMA4GA1UECgwHSmF2YWxpbjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC
AQoCggEBANgf9ywzZUgab2y9WIJTf4H6rxMITbRDC3xYJRVEHc8WgegrlPwXw4V/
Qr92xChowUKZBQX98yvLXMc0YziRmqY+m5IriaWY7Jvr82pkHkLKWyjzFmHZBMAd
lWWjDKDD5abKPhsPu4tJg3YedRm7SjZIm7rpj+rEau4ALGRonM8L4jpqZJ+Jg8Sq
DDKTnVGLiYuhxtmby+/PRV/mhmJJ6dPOOcdIQlIn1PCrUDJbt2zMkuflrVzl+6eY
7GYq5h+QqCgO2XK+6q5RQBxtMUIp8Bi0AR6j+g3yAE6uiAPXhDQOj+fzKRJhoYcf
cQ84yxzN4benHPPPfLw42rquA7qB/BECAwEAAaN/MH0wHQYDVR0OBBYEFCIswSAR
oq1RbOP1ygckApt9wHPKMB8GA1UdIwQYMBaAFCIswSARoq1RbOP1ygckApt9wHPK
MA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIw
DAYDVR0TAQH/BAIwADANBgkqhkiG9w0BAQsFAAOCAQEAi4zGlp2DmaH9sMiRgqA5
dDO8UbrW1TNNES7Dwo1519sERiosCzGVBqjVOzUzFYyrW/jF8kKd8NoIZSlWvoeQ
A0+6dxYy7oNY0UTJbW25hSRXMF1FCnxBfLLZ1J9lowzhts3yx5REJZVWEvsF0Agy
qgNEkKYSaeUtuSzUhMVPGs9AuMwl/M0M1q+2WBMeDLaGXhAXJC5jZ47BkEVgnz5+
/IIWbFJQ+eGEgL+GVFCxgebvJwncPruDipaS7i486kYyoymBKiSXeUN+z+gdaIHk
YHuSkRVAU4BiSGd72UK+KWIYBttkeINcYLRyZbdYkY5sgBGTJnj2ke0vnM13o7UM
jw==
-----END CERTIFICATE-----
"""
    const val GOOGLE_KEY_AS_STRING =
        """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDYH/csM2VIGm9s
vViCU3+B+q8TCE20Qwt8WCUVRB3PFoHoK5T8F8OFf0K/dsQoaMFCmQUF/fMry1zH
NGM4kZqmPpuSK4mlmOyb6/NqZB5Cylso8xZh2QTAHZVlowygw+Wmyj4bD7uLSYN2
HnUZu0o2SJu66Y/qxGruACxkaJzPC+I6amSfiYPEqgwyk51Ri4mLocbZm8vvz0Vf
5oZiSenTzjnHSEJSJ9Twq1AyW7dszJLn5a1c5funmOxmKuYfkKgoDtlyvuquUUAc
bTFCKfAYtAEeo/oN8gBOrogD14Q0Do/n8ykSYaGHH3EPOMsczeG3pxzzz3y8ONq6
rgO6gfwRAgMBAAECggEAOCrWid4xjDOSkagDwJsCoD0OEtwtlZN3ALHHsWcqeA9Z
Y4UwCvQCFEemiSvMftP6pdwuugftkowfaIXs416z2lCbDbnS4/6CP2Nqt1Odqa39
Uv8Z6gQEgAkwMmHVflJq9JXK3i2Qh/pq99+ifzV1a/YiwsjAZjr1rzTMVKv7VLM/
cRwEffg/kus8b/RDUVDcTFlX2drypfAI9T2P7kaiMLVKI7tcWnn1RPUdUqlG8LjB
4ZyyZXNGNrd93zYWWe1DflUwP187SyoGbsn0vQe15n0U7cVOE8xLOBNFJiKSUJGi
hdUUeIgXZS9MGTfQN6QldRsFNlv6DGDvSzocqtl7gQKBgQD6P96IbtQeD/UwVKb1
1KzvDzdzlxT4XG7e2ZPj71tG9d84V72PDPAYnzftPJRkD08lDJLvAvKqqKCPTX3V
QgauF2njX1XLa649E4/hb5VHnJQj9Fwlx3/KUaNWI8zu3PR+5gKp/dlJiJz0v84T
gNw/VYhzfXyjPtbJXYrij8QpRQKBgQDdF1pbBKDAkBwoiOa2jgBtjnzHm7SxLCqq
/BBsKT54q8yxdTsY6MZ4J+497Z+AL65zHKMgcDsMEOvKDqLhurvqvWzUGTCv/5St
xOuwJ0k4kIVrQEBvvAJmBT44WOQZM3M9vxbwoG2mjfsSPz7OdrkK+hnwBlu91AjK
mP9rKa/mXQKBgQDYbfSgOnnpphOAQTZE1jLabmae6cORKSAaTELDl3dx36O2ruua
lK3yHYHZA9Oy1iq0+DL706jcQArc5UA2+GuelVFW/FTPIcoHuKtvZXnN/XWBww0O
/4NeD00casoKq74pIfSb4JfUKPrWEizAYWoavHbOq3DoHqjUbrp3R693oQKBgA7i
T5bpDNlp2jtwW/fWP3kgqo3VkaiLzKOOLJzbefUtu64Gsl/O6+2S4psQsDg0/Y2K
VAEPDSqWyQjlS1ne9F+tOPJeb8SpdBzusN8/BdLlB9ZckPn0skSj/bhVY6W+rPdv
MeApLLiVvl1QHK5Rl8uBYtWh1/NDnwPkoO1Z9RmRAoGALzIURD5Dg9FCpd9ym+UZ
JWI9lPvbL3uBzD53ys0dtzoSaTayishooeggYYJEbYpAxNH0WE1M7dqZH/OjTaAO
Kp7LluqrRTUGMYHogBX485sCWhZ91r4RqPa90UcUcpjXUnVu7Absn7/FOcT1z++M
6HNWxu22y49Nc0iAEtqCOVk=
-----END PRIVATE KEY-----
"""
    const val CERTIFICATE_FILE_NAME = "server/cert.crt"
    const val ENCRYPTED_KEY_FILE_NAME = "server/encrypted.key"
    const val NON_ENCRYPTED_KEY_FILE_NAME = "server/passwordless.key"
    val CERTIFICATE_PATH: String
    val ENCRYPTED_KEY_PATH: String
    val NON_ENCRYPTED_KEY_PATH: String
    const val KEY_PASSWORD = "password"
    const val JKS_KEY_STORE_NAME = "server/keystore.jks"
    val JKS_KEY_STORE_INPUT_STREAM_SUPPLIER = Supplier {
        try {
            return@Supplier ClassLoader.getSystemResource(JKS_KEY_STORE_NAME).openStream()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    const val P12_KEY_STORE_NAME = "server/keystore.p12"
    val P12_KEY_STORE_INPUT_STREAM_SUPPLIER = Supplier {
        try {
            return@Supplier ClassLoader.getSystemResource(P12_KEY_STORE_NAME).openStream()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    const val NORWAY_JKS_KEY_STORE_NAME = "server/norway.jks"
    const val NORWAY_P12_KEY_STORE_NAME = "server/norway.p12"
    @JvmField
    val JKS_KEY_STORE_PATH: String
    @JvmField
    val P12_KEY_STORE_PATH: String
    @JvmField
    val NORWAY_JKS_KEY_STORE_PATH: String
    @JvmField
    val NORWAY_P12_KEY_STORE_PATH: String
    const val KEY_STORE_PASSWORD = "password"

    init {
        try {
            CERTIFICATE_PATH =
                Path.of(ClassLoader.getSystemResource(CERTIFICATE_FILE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            ENCRYPTED_KEY_PATH =
                Path.of(ClassLoader.getSystemResource(ENCRYPTED_KEY_FILE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            NON_ENCRYPTED_KEY_PATH =
                Path.of(ClassLoader.getSystemResource(NON_ENCRYPTED_KEY_FILE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            JKS_KEY_STORE_PATH =
                Path.of(ClassLoader.getSystemResource(JKS_KEY_STORE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            P12_KEY_STORE_PATH =
                Path.of(ClassLoader.getSystemResource(P12_KEY_STORE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            NORWAY_JKS_KEY_STORE_PATH =
                Path.of(ClassLoader.getSystemResource(NORWAY_JKS_KEY_STORE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    init {
        try {
            NORWAY_P12_KEY_STORE_PATH =
                Path.of(ClassLoader.getSystemResource(NORWAY_P12_KEY_STORE_NAME).toURI()).toAbsolutePath().toString()
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }
}
