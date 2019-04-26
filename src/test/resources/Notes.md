# Test Data
## Generating Keys
We are using openssl to generate our public and private keys.  Note that because these keys are being made available as part of a public repository, they should never be used in production.

```
openssl genpkey -algorithm RSA -out private-key-1.pem -pkeyopt rsa_keygen_bits:2048
chmod go-r private-key-1.pem
openssl rsa -pubout -in private-key-1.pem -out public-key-1.pem
```

This (in order) generates a 2048-bit private key using RSA, changes the permissions (on a macOS/Linux system) so others can't read the private key, and then generates the public key.
Changing the permissions on the private key is important in production, and is included here for documentation purposes.

If you want to script this (like we did here) to generate multiple keys:

```
for run in {1..3}
do
  openssl genpkey -algorithm RSA -out private-key-$run.pem -pkeyopt rsa_keygen_bits:2048
  chmod go-r private-key-$run.pem
  openssl rsa -pubout -in private-key-1.pem -out public-key-$run.pem
done
```