/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pdfbox.pdmodel.encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.encryption.ARCFour;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.WrappedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * This class represents a security handler as described in the PDF specifications. A security handler is responsible of
 * documents protection.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @author Benoit Guillon (benoit.guillon@snv.jussieu.fr)
 *
 */

public abstract class SecurityHandler
{

    /**
     * CONSTANTS.
     */
    private static final Log LOG = LogFactory.getLog(SecurityHandler.class);

    private static final int DEFAULT_KEY_LENGTH = 40;

    /*
     * See 7.6.2, page 58, PDF 32000-1:2008
     */
    private static final byte[] AES_SALT = { (byte) 0x73, (byte) 0x41, (byte) 0x6c, (byte) 0x54 };

    /**
     * The value of V field of the Encryption dictionary.
     */
    protected int version;

    /**
     * The length of the secret key used to encrypt the document.
     */
    protected int keyLength = DEFAULT_KEY_LENGTH;

    /**
     * The encryption key that will used to encrypt / decrypt.
     */
    protected byte[] encryptionKey;

    /**
     * The document whose security is handled by this security handler.
     */

    protected PDDocument document;

    /**
     * The RC4 implementation used for cryptographic functions.
     */
    protected ARCFour rc4 = new ARCFour();

    /**
     * indicates if the Metadata have to be decrypted of not
     */
    protected boolean decryptMetadata;
    private Set<COSBase> objects = new HashSet<COSBase>();

    private Set<COSDictionary> potentialSignatures = new HashSet<COSDictionary>();

    /**
     * If true, AES will be used.
     */
    private boolean aes;

    /**
     * The access permission granted to the current user for the document. These permissions are computed during
     * decryption and are in read only mode.
     */

    protected AccessPermission currentAccessPermission = null;

    /**
     * Prepare the document for encryption.
     *
     * @param doc
     *            The document that will be encrypted.
     *
     * @throws CryptographyException
     *             If there is an error while preparing.
     * @throws IOException
     *             If there is an error with the document.
     */
    public abstract void prepareDocumentForEncryption(PDDocument doc) throws CryptographyException, IOException;

    /**
     * Prepares everything to decrypt the document.
     * 
     * If {@link #decryptDocument(PDDocument, DecryptionMaterial)} is used, this method is called from there. Only if
     * decryption of single objects is needed this should be called instead.
     *
     * @param encDictionary
     *            encryption dictionary, can be retrieved via {@link PDDocument#getEncryptionDictionary()}
     * @param documentIDArray
     *            document id which is returned via {@link COSDocument#getDocumentID()}
     * @param decryptionMaterial
     *            Information used to decrypt the document.
     *
     * @throws IOException
     *             If there is an error accessing data.
     * @throws CryptographyException
     *             If there is an error with decryption.
     */
    public abstract void prepareForDecryption(PDEncryptionDictionary encDictionary, COSArray documentIDArray,
            DecryptionMaterial decryptionMaterial) throws CryptographyException, IOException;

    /**
     * Prepare the document for decryption.
     *
     * @param doc
     *            The document to decrypt.
     * @param mat
     *            Information required to decrypt the document.
     * @throws CryptographyException
     *             If there is an error while preparing.
     * @throws IOException
     *             If there is an error with the document.
     */
    public abstract void decryptDocument(PDDocument doc, DecryptionMaterial mat) throws CryptographyException,
            IOException;

    /**
     * This method must be called by an implementation of this class to really proceed to decryption.
     *
     * @throws IOException
     *             If there is an error in the decryption.
     * @throws CryptographyException
     *             If there is an error in the decryption.
     */
    protected void proceedDecryption() throws IOException, CryptographyException
    {

        COSDictionary trailer = document.getDocument().getTrailer();
        COSArray fields = (COSArray) trailer.getObjectFromPath("Root/AcroForm/Fields");

        // We need to collect all the signature dictionaries, for some
        // reason the 'Contents' entry of signatures is not really encrypted
        if (fields != null)
        {
            for (int i = 0; i < fields.size(); i++)
            {
                COSDictionary field = (COSDictionary) fields.getObject(i);
                if (field != null)
                {
                    addDictionaryAndSubDictionary(potentialSignatures, field);
                }
                else
                {
                    throw new IOException("Could not decypt document, object not found.");
                }
            }
        }

        List<COSObject> allObjects = document.getDocument().getObjects();
        Iterator<COSObject> objectIter = allObjects.iterator();
        COSDictionary encryptionDict = document.getEncryptionDictionary().getCOSDictionary();
        while (objectIter.hasNext())
        {
            COSObject nextObj = objectIter.next();
            COSBase nextCOSBase = nextObj.getObject();
            if (nextCOSBase != encryptionDict)
            {
                decryptObject(nextObj);
            }
        }
        document.setEncryptionDictionary(null);
    }

    private void addDictionaryAndSubDictionary(Set<COSDictionary> set, COSDictionary dic)
    {
        if (dic != null) // in case dictionary is part of object stream we have null value here
        {
            set.add(dic);
            COSArray kids = (COSArray) dic.getDictionaryObject(COSName.KIDS);
            for (int i = 0; kids != null && i < kids.size(); i++)
            {
                addDictionaryAndSubDictionary(set, (COSDictionary) kids.getObject(i));
            }
            COSBase value = dic.getDictionaryObject(COSName.V);
            if (value instanceof COSDictionary)
            {
                addDictionaryAndSubDictionary(set, (COSDictionary) value);
            }
        }
    }

    /**
     * Encrypt a set of data.
     *
     * @param objectNumber
     *            The data object number.
     * @param genNumber
     *            The data generation number.
     * @param data
     *            The data to encrypt.
     * @param output
     *            The output to write the encrypted data to.
     * @throws CryptographyException
     *             If there is an error during the encryption.
     * @throws IOException
     *             If there is an error reading the data.
     * @deprecated While this works fine for RC4 encryption, it will never decrypt AES data You should use
     *             encryptData(objectNumber, genNumber, data, output, decrypt) which can do everything. This function is
     *             just here for compatibility reasons and will be removed in the future.
     */
    public void encryptData(long objectNumber, long genNumber, InputStream data, OutputStream output)
            throws CryptographyException, IOException
    {
        // default to encrypting since the function is named "encryptData"
        encryptData(objectNumber, genNumber, data, output, false);
    }

    /**
     * Encrypt a set of data.
     *
     * @param objectNumber
     *            The data object number.
     * @param genNumber
     *            The data generation number.
     * @param data
     *            The data to encrypt.
     * @param output
     *            The output to write the encrypted data to.
     * @param decrypt
     *            true to decrypt the data, false to encrypt it
     *
     * @throws CryptographyException
     *             If there is an error during the encryption.
     * @throws IOException
     *             If there is an error reading the data.
     */
    public void encryptData(long objectNumber, long genNumber, InputStream data, OutputStream output, boolean decrypt)
            throws CryptographyException, IOException
    {
        if (aes && !decrypt)
        {
            throw new IllegalArgumentException("AES encryption is not yet implemented.");
        }

        byte[] newKey = new byte[encryptionKey.length + 5];
        System.arraycopy(encryptionKey, 0, newKey, 0, encryptionKey.length);
        // PDF 1.4 reference pg 73
        // step 1
        // we have the reference

        // step 2
        newKey[newKey.length - 5] = (byte) (objectNumber & 0xff);
        newKey[newKey.length - 4] = (byte) ((objectNumber >> 8) & 0xff);
        newKey[newKey.length - 3] = (byte) ((objectNumber >> 16) & 0xff);
        newKey[newKey.length - 2] = (byte) (genNumber & 0xff);
        newKey[newKey.length - 1] = (byte) ((genNumber >> 8) & 0xff);

        // step 3
        byte[] digestedKey = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(newKey);
            if (aes)
            {
                md.update(AES_SALT);
            }
            digestedKey = md.digest();
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new CryptographyException(e);
        }

        // step 4
        int length = Math.min(newKey.length, 16);
        byte[] finalKey = new byte[length];
        System.arraycopy(digestedKey, 0, finalKey, 0, length);

        if (aes)
        {
            byte[] iv = new byte[16];

            data.read(iv);

            try
            {
                Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKey aesKey = new SecretKeySpec(finalKey, "AES");
                IvParameterSpec ips = new IvParameterSpec(iv);
                decryptCipher.init(decrypt ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE, aesKey, ips);

                byte[] buffer = new byte[256];
                for (int n = 0; -1 != (n = data.read(buffer));)
                {
                    output.write(decryptCipher.update(buffer, 0, n));
                }
                output.write(decryptCipher.doFinal());
            }
            catch (InvalidKeyException e)
            {
                throw new WrappedIOException(e.getMessage(), e);
            }
            catch (InvalidAlgorithmParameterException e)
            {
                throw new WrappedIOException(e.getMessage(), e);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new WrappedIOException(e.getMessage(), e);
            }
            catch (NoSuchPaddingException e)
            {
                throw new WrappedIOException(e.getMessage(), e);
            }
            catch (IllegalBlockSizeException e)
            {
                throw new WrappedIOException(e.getMessage(), e);
            }
            catch (BadPaddingException e)
            {
                throw new WrappedIOException(e.getMessage(), e);
            }
        }
        else
        {
            rc4.setKey(finalKey);
            rc4.write(data, output);
        }

        output.flush();
    }

    /**
     * This will decrypt an object in the document.
     *
     * @param object
     *            The object to decrypt.
     *
     * @throws CryptographyException
     *             If there is an error decrypting the stream.
     * @throws IOException
     *             If there is an error getting the stream data.
     */
    private void decryptObject(COSObject object) throws CryptographyException, IOException
    {
        long objNum = object.getObjectNumber().intValue();
        long genNum = object.getGenerationNumber().intValue();
        COSBase base = object.getObject();
        decrypt(base, objNum, genNum);
    }

    /**
     * This will dispatch to the correct method.
     *
     * @param obj
     *            The object to decrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation Number.
     *
     * @throws CryptographyException
     *             If there is an error decrypting the stream.
     * @throws IOException
     *             If there is an error getting the stream data.
     */
    private void decrypt(COSBase obj, long objNum, long genNum) throws CryptographyException, IOException
    {
        if (!objects.contains(obj))
        {
            objects.add(obj);

            if (obj instanceof COSString)
            {
                decryptString((COSString) obj, objNum, genNum);
            }
            else if (obj instanceof COSStream)
            {
                decryptStream((COSStream) obj, objNum, genNum);
            }
            else if (obj instanceof COSDictionary)
            {
                decryptDictionary((COSDictionary) obj, objNum, genNum);
            }
            else if (obj instanceof COSArray)
            {
                decryptArray((COSArray) obj, objNum, genNum);
            }
        }
    }

    /**
     * This will decrypt a stream.
     *
     * @param stream
     *            The stream to decrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation number.
     *
     * @throws CryptographyException
     *             If there is an error getting the stream.
     * @throws IOException
     *             If there is an error getting the stream data.
     */
    public void decryptStream(COSStream stream, long objNum, long genNum) throws CryptographyException, IOException
    {
        COSBase type = stream.getDictionaryObject(COSName.TYPE);
        if (!decryptMetadata && COSName.METADATA.equals(type))
        {
            return;
        }
        // "The cross-reference stream shall not be encrypted"
        if (COSName.XREF.equals(type))
        {
            return;
        }
        if (COSName.METADATA.equals(type))
        {
            // PDFBOX-3229 check case where metadata is not encrypted despite /EncryptMetadata missing
            InputStream is = stream.getFilteredStream();
            byte buf[] = new byte[10];
            is.read(buf);
            is.close();
            if (Arrays.equals(buf, "<?xpacket ".getBytes("ISO_8859_1")))
            {
                LOG.warn("Metadata is not encrypted, but was expected to be");
                LOG.warn("Read PDF specification about EncryptMetadata (default value: true)");
                return;
            }
        }
        decryptDictionary(stream, objNum, genNum);
        InputStream encryptedStream = stream.getFilteredStream();
        encryptData(objNum, genNum, encryptedStream, stream.createFilteredStream(), true /* decrypt */);
    }

    /**
     * This will encrypt a stream, but not the dictionary as the dictionary is encrypted by visitFromString() in
     * COSWriter and we don't want to encrypt it twice.
     *
     * @param stream
     *            The stream to decrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation number.
     *
     * @throws CryptographyException
     *             If there is an error getting the stream.
     * @throws IOException
     *             If there is an error getting the stream data.
     */
    public void encryptStream(COSStream stream, long objNum, long genNum) throws CryptographyException, IOException
    {
        InputStream encryptedStream = stream.getFilteredStream();
        encryptData(objNum, genNum, encryptedStream, stream.createFilteredStream(), false /* encrypt */);
    }

    /**
     * This will decrypt a dictionary.
     *
     * @param dictionary
     *            The dictionary to decrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation number.
     *
     * @throws CryptographyException
     *             If there is an error decrypting the document.
     * @throws IOException
     *             If there is an error creating a new string.
     */
    private void decryptDictionary(COSDictionary dictionary, long objNum, long genNum) throws CryptographyException,
            IOException
    {
        COSBase type = dictionary.getDictionaryObject(COSName.TYPE);
        boolean isSignature = COSName.SIG.equals(type) || COSName.DOC_TIME_STAMP.equals(type);
        for (Map.Entry<COSName, COSBase> entry : dictionary.entrySet())
        {
            if (isSignature && COSName.CONTENTS.equals(entry.getKey()))
            {
                // do not decrypt the signature contents string
                continue;
            }
            COSBase value = entry.getValue();
            // within a dictionary only the following kind of COS objects have to be decrypted
            if (value instanceof COSString || value instanceof COSStream || value instanceof COSArray
                    || value instanceof COSDictionary)
            {
                // if we are a signature dictionary and contain a Contents entry then
                // we don't decrypt it.
                if (!(entry.getKey().equals(COSName.CONTENTS) && value instanceof COSString && potentialSignatures
                        .contains(dictionary)))
                {
                    decrypt(value, objNum, genNum);
                }
            }
        }
    }

    /**
     * This will encrypt a string.
     *
     * @param string
     *            the string to encrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation number.
     *
     * @throws IOException
     *             If an error occurs writing the new string.
     */
    public void encryptString(COSString string, long objNum, long genNum) throws CryptographyException, IOException
    {
        ByteArrayInputStream data = new ByteArrayInputStream(string.getBytes());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        encryptData(objNum, genNum, data, buffer, false /* decrypt */);
        string.reset();
        string.append(buffer.toByteArray());
    }

    /**
     * This will decrypt a string.
     *
     * @param string
     *            the string to decrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation number.
     *
     * @throws CryptographyException
     *             If an error occurs during decryption.
     * @throws IOException
     *             If an error occurs writing the new string.
     */
    public void decryptString(COSString string, long objNum, long genNum) throws CryptographyException, IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(string.getBytes());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            encryptData(objNum, genNum, bais, baos, true /* decrypt */);
            string.reset();
            string.append(baos.toByteArray());
        }
        catch (WrappedIOException ex)
        {
            LOG.error("Failed to decrypt COSString of length " + string.getBytes().length + 
                    " in object " + objNum + ": " + ex.getMessage());
        }
    }

    /**
     * This will decrypt an array.
     *
     * @param array
     *            The array to decrypt.
     * @param objNum
     *            The object number.
     * @param genNum
     *            The object generation number.
     *
     * @throws CryptographyException
     *             If an error occurs during decryption.
     * @throws IOException
     *             If there is an error accessing the data.
     */
    public void decryptArray(COSArray array, long objNum, long genNum) throws CryptographyException, IOException
    {
        for (int i = 0; i < array.size(); i++)
        {
            decrypt(array.get(i), objNum, genNum);
        }
    }

    /**
     * Getter of the property <tt>keyLength</tt>.
     * 
     * @return Returns the keyLength.
     */
    public int getKeyLength()
    {
        return keyLength;
    }

    /**
     * Setter of the property <tt>keyLength</tt>.
     *
     * @param keyLen
     *            The keyLength to set.
     */
    public void setKeyLength(int keyLen)
    {
        this.keyLength = keyLen;
    }

    /**
     * Returns the access permissions that were computed during document decryption. The returned object is in read only
     * mode.
     *
     * @return the access permissions or null if the document was not decrypted.
     */
    public AccessPermission getCurrentAccessPermission()
    {
        return currentAccessPermission;
    }

    /**
     * True if AES is used for encryption and decryption.
     * 
     * @return true if AEs is used
     */
    public boolean isAES()
    {
        return aes;
    }

    /**
     * Set to true if AES for encryption and decryption should be used.
     * 
     * @param aesValue
     *            if true AES will be used
     * 
     */
    public void setAES(boolean aesValue)
    {
        aes = aesValue;
    }
 
    /**
     * Returns whether a protection policy has been set.
     * 
     * @return true if a protection policy has been set.
     */
    public abstract boolean hasProtectionPolicy();
}