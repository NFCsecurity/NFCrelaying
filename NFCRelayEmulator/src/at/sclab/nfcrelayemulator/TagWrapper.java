/* Copyright 2012 Nikolay Elenkov
   https://github.com/nelenkov/virtual-pki-card 
   slightly modified by Michael Heinzl, Stefan Peherstorfer and Georg Chalupar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package at.sclab.nfcrelayemulator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.nfc.Tag;
import android.nfc.tech.TagTechnology;

public class TagWrapper implements TagTechnology {

    private Method isConnected;
    private Method connect;
    private Method reconnect;
    private Method close;
    private Method getMaxTransceiveLength;
    private Method transceive;

    private Tag tag;
    private Object tagTech;

    public TagWrapper(Tag tag, String tech) {
        try {
            this.tag = tag;

            Class<?> cls = Class.forName(tech);
            Method get = cls.getMethod("get", Tag.class);
            tagTech = get.invoke(null, tag);

            isConnected = cls.getMethod("isConnected");
            connect = cls.getMethod("connect");
            reconnect = cls.getMethod("reconnect");
            close = cls.getMethod("close");
            getMaxTransceiveLength = cls.getMethod("getMaxTransceiveLength");
            transceive = cls.getMethod("transceive", byte[].class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsRE(e);
        }
    }

    private void throwCauseAsRE(InvocationTargetException e) {
        if (e.getTargetException() != null) {
            throw new RuntimeException(e.getTargetException());
        }

        throw new RuntimeException(e);
    }

    private void throwCauseAsIOE(InvocationTargetException e)
            throws IOException {
        if (e.getTargetException() != null) {
            if (e.getTargetException() instanceof IOException) {
                throw (IOException) e.getTargetException();
            } else {
                throw new IOException("exception invoking method", e.getTargetException());
            }
        }

        throw new IOException("target exception was null", e);
    }

    @Override
    public boolean isConnected() {
        boolean result = false;
        try {
            return (Boolean) isConnected.invoke(tagTech);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsRE(e);
        }

        return result;
    }

    @Override
    public void connect() throws IOException {
        try {
            connect.invoke(tagTech);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsIOE(e);
        }
    }

    //    @Override @hide-n, so can't use annotation
    public void reconnect() throws IOException {
        try {
            reconnect.invoke(tagTech);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsIOE(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            close.invoke(tagTech);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsIOE(e);
        }
    }

    public int getMaxTransceiveLength() {
        try {
            return (Integer) getMaxTransceiveLength.invoke(tagTech);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsRE(e);
        }

        return 0;
    }

    public byte[] transceive(byte[] data) throws IOException {
        try {
            return (byte[]) transceive.invoke(tagTech, data);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throwCauseAsIOE(e);
        }
        throw new IOException(); // should never reach this
    }

    @Override
    public Tag getTag() {
        return tag;
    }
}
