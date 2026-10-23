package com.github.druk.dnssd;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TXTRecord packs key=value pairs as length-prefixed strings (DNS-SD, section 6). Plain JVM test:
 * no native code involved.
 */
public class TXTRecordTest {

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    public void emptyRecordHasNoKeysAndNoBytes() {
        TXTRecord record = new TXTRecord();

        assertEquals(0, record.size());
        assertEquals(0, record.getRawBytes().length);
        assertNull(record.getKey(0));
        assertNull(record.getValue(0));
        assertEquals("", record.toString());
    }

    @Test
    public void setWritesALengthPrefixedKeyValuePair() {
        TXTRecord record = new TXTRecord();

        record.set("txtvers", "1");

        byte[] expected = new byte[10];
        expected[0] = 9;
        System.arraycopy(ascii("txtvers=1"), 0, expected, 1, 9);
        assertArrayEquals(expected, record.getRawBytes());
        assertEquals(1, record.size());
        assertEquals("txtvers", record.getKey(0));
        assertEquals("1", record.getValueAsString(0));
        assertEquals("1", record.getValueAsString("txtvers"));
    }

    @Test
    public void keyWithoutValueIsPresentButHasNullValue() {
        TXTRecord record = new TXTRecord();

        record.set("flag", (String) null);

        assertArrayEquals(new byte[]{4, 'f', 'l', 'a', 'g'}, record.getRawBytes());
        assertTrue(record.contains("flag"));
        assertNull(record.getValue("flag"));
        assertNull(record.getValueAsString(0));
        assertEquals("0={flag}", record.toString());
    }

    @Test
    public void keyWithEmptyValueHasZeroLengthValue() {
        TXTRecord record = new TXTRecord();

        record.set("empty", "");

        assertArrayEquals(new byte[]{6, 'e', 'm', 'p', 't', 'y', '='}, record.getRawBytes());
        assertEquals(0, record.getValue("empty").length);
        assertEquals("", record.getValueAsString("empty"));
    }

    @Test
    public void newKeysAreAppendedInOrder() {
        TXTRecord record = new TXTRecord();

        record.set("a", "1");
        record.set("b", "2");
        record.set("c", "3");

        assertEquals(3, record.size());
        assertEquals("a", record.getKey(0));
        assertEquals("b", record.getKey(1));
        assertEquals("c", record.getKey(2));
        assertNull(record.getKey(3));
        assertEquals("0={a=1}, 1={b=2}, 2={c=3}", record.toString());
    }

    @Test
    public void settingAnExistingKeyReplacesItInPlaceIgnoringCase() {
        TXTRecord record = new TXTRecord();
        record.set("a", "1");
        record.set("b", "2");

        record.set("A", "3");

        assertEquals(2, record.size());
        assertEquals("A", record.getKey(0));
        assertEquals("3", record.getValueAsString("a"));
        assertEquals("b", record.getKey(1));
    }

    @Test
    public void lookupsIgnoreKeyCase() {
        TXTRecord record = new TXTRecord();
        record.set("Path", "/index.html");

        assertTrue(record.contains("PATH"));
        assertEquals("/index.html", record.getValueAsString("path"));
        assertFalse(record.contains("pat"));
        assertNull(record.getValue("missing"));
    }

    @Test
    public void removeReturnsTheIndexAndDropsThePair() {
        TXTRecord record = new TXTRecord();
        record.set("a", "1");
        record.set("b", "2");

        assertEquals(1, record.remove("B"));

        assertEquals(1, record.size());
        assertFalse(record.contains("b"));
        assertEquals(-1, record.remove("b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void keyContainingEqualsSignIsRejected() {
        new TXTRecord().set("a=b", "1");
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void pairOf255BytesOrMoreIsRejected() {
        char[] value = new char[254];
        Arrays.fill(value, 'v');
        new TXTRecord().set("k", new String(value));
    }

    @Test
    public void parsesWireFormatBytes() {
        byte[] wire = {3, 'a', '=', '1', 4, 'f', 'l', 'a', 'g'};

        TXTRecord record = new TXTRecord(wire);

        assertEquals(2, record.size());
        assertEquals("1", record.getValueAsString("a"));
        assertTrue(record.contains("flag"));
    }

    @Test
    public void keepsItsOwnCopyOfTheBytes() {
        byte[] wire = {3, 'a', '=', '1'};
        TXTRecord record = new TXTRecord(wire);

        wire[3] = '2';
        record.getRawBytes()[3] = '3';

        assertEquals("1", record.getValueAsString("a"));
    }

    // Pins current behaviour. getKey()/getValue()/remove() read the length byte as a signed byte,
    // so a pair of 128..254 bytes (accepted by set()) reads back as an empty key with no value,
    // remove() cannot find it, and a lookup by key walks to a negative offset and throws. size()
    // masks the byte and still counts it. Suspected bug, also in Apple's original TXTRecord; see
    // docs/runbooks/testing.md.
    @Test
    public void pairLongerThan127BytesReadsBackAsAnEmptyKey() {
        char[] value = new char[150];
        Arrays.fill(value, 'v');
        TXTRecord record = new TXTRecord();

        record.set("k", new String(value));

        assertEquals(1, record.size());
        assertEquals("", record.getKey(0));
        assertNull(record.getValue(0));
        assertEquals(-1, record.remove("k"));
        try {
            record.getValue("k");
            fail("expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // signed length byte: the walk to the second key steps backwards
        }
    }

    @Test
    public void parseTXTRecordsMapsEveryKeyToItsValue() {
        TXTRecord record = new TXTRecord();
        record.set("txtvers", "1");
        record.set("flag", (String) null);

        Map<String, String> map = DNSSD.parseTXTRecords(record.getRawBytes());

        assertEquals(2, map.size());
        assertEquals("1", map.get("txtvers"));
        assertTrue(map.containsKey("flag"));
        assertNull(map.get("flag"));
    }
}
