An encoder and decoder implemented by java for the "VCDIFF Generic Differencing and Compression Data Format" described by [RFC 3284] .

This project is mainly from the c++ implemention: [open-vcdiff]. 
The encoding strategy is largely based on Bentley-McIlroy 99: "[Data Compression Using Long Common Strings]."

If maven is used, add:
```
        <dependency>
            <groupId>net.dongliu</groupId>
            <artifactId>jvcdiff</artifactId>
            <version>1.2.3</version>
        </dependency>
```
to your pom.xml file.

The lib has a simple API, go though VcdiffEncode / VcdiffDecode to get the usage.

[RFC 3284]: http://www.ietf.org/rfc/rfc3284.txt  "RFC 3284"
[Data Compression Using Long Common Strings]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.11.8470&rep=rep1&type=pdf "Data Compression Using Long Common Strings"
[open-vcdiff]: https://code.google.com/p/open-vcdiff/ "open-vcdiff"
