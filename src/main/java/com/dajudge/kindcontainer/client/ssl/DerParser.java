package com.dajudge.kindcontainer.client.ssl;

// The following part was copied over and stripped down from
// https://github.com/blindsidenetworks/oauth/blob/504fa4f4b5bc0385d05de0ee3c66d1f27c014adb/src/main/java/net/oauth/signature/pem/DerParser.java
// https://github.com/blindsidenetworks/oauth/blob/504fa4f4b5bc0385d05de0ee3c66d1f27c014adb/src/main/java/net/oauth/signature/pem/Asn1Object.java
/* **************************************************************************
 * Copyright (c) 1998-2009 AOL LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *************************************************************************** */

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

/**
 * A bare-minimum ASN.1 DER decoder, just having enough functions to
 * decode PKCS#1 private keys. Especially, it doesn't handle explicitly
 * tagged types with an outer tag.
 * <p>
 * <p/>This parser can only handle one layer. To parse nested constructs,
 * get a new parser for each layer using <code>Asn1Object.getParser()</code>.
 * <p>
 * <p/>There are many DER decoders in JRE but using them will tie this
 * program to a specific JCE/JVM.
 *
 * @author zhang
 */
class DerParser {

    public final static int ANY = 0x00;
    public final static int INTEGER = 0x02;

    protected InputStream in;

    public DerParser(InputStream in) {
        this.in = in;
    }

    public Asn1Object read() throws IOException {
        int tag = in.read();

        if (tag == -1)
            throw new IOException("Invalid DER: stream too short, missing tag");

        int length = getLength();

        byte[] value = new byte[length];
        int n = in.read(value);
        if (n < length)
            throw new IOException("Invalid DER: stream too short, missing value");

        return new Asn1Object(tag, length, value);
    }

    private int getLength() throws IOException {

        int i = in.read();
        if (i == -1)
            throw new IOException("Invalid DER: length missing");

        if ((i & ~0x7F) == 0)
            return i;

        int num = i & 0x7F;

        if (i >= 0xFF || num > 4)
            throw new IOException("Invalid DER: length field too big (" + i + ")");

        byte[] bytes = new byte[num];
        int n = in.read(bytes);
        if (n < num)
            throw new IOException("Invalid DER: length too short");

        return new BigInteger(1, bytes).intValue();
    }

    /**
     * An ASN.1 TLV. The object is not parsed. It can
     * only handle integers and strings.
     *
     * @author zhang
     */
    static class Asn1Object {

        protected final int type;
        protected final int length;
        protected final byte[] value;
        protected final int tag;

        public Asn1Object(int tag, int length, byte[] value) {
            this.tag = tag;
            this.type = tag & 0x1F;
            this.length = length;
            this.value = value;
        }

        public int getType() {
            return type;
        }

        public byte[] getValue() {
            return value;
        }

        public BigInteger getBigInteger() throws IOException {
            if (type != INTEGER)
                throw new IOException("Invalid DER: object is not integer");

            return new BigInteger(value);
        }
    }
}
