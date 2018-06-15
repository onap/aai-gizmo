/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.crud.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/**
 * Generates a sha 256 hash
 */
public class HashGenerator {

    private MessageDigest messageDigest;

    public HashGenerator() throws NoSuchAlgorithmException {
        this.messageDigest = MessageDigest.getInstance("SHA-256");
    }

    /**
     * Generates a SHA 256 hash as a hexadecimal string for the inputs.
     * Calls toString on the input objects to convert into a byte stream.
     * @param values
     * @return SHA 256 hash of the inputs as a hexadecimal string.
     * @throws IOException
     */
    public String generateSHA256AsHex(Object... values) throws IOException {
        byte[] bytes = convertToBytes(values);
        byte[] digest = messageDigest.digest(bytes);
        StringBuilder result = new StringBuilder();
        for (byte byt : digest) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    private byte[] convertToBytes(Object... values) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            for (Object object : values) {
                out.writeObject(object.toString());
            }
            return bos.toByteArray();
        }
    }

}