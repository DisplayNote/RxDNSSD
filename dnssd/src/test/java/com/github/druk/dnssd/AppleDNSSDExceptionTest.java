package com.github.druk.dnssd;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Error codes from the native layer and the messages built from them. */
public class AppleDNSSDExceptionTest {

    @Test
    public void keepsTheErrorCode() {
        DNSSDException e = new AppleDNSSDException(DNSSDException.NAME_CONFLICT);

        assertEquals(DNSSDException.NAME_CONFLICT, e.getErrorCode());
        assertTrue(e instanceof Exception);
    }

    @Test
    public void namesTheFirstKnownCode() {
        assertEquals("DNS-SD Error -65537: UNKNOWN",
                new AppleDNSSDException(DNSSDException.UNKNOWN).getMessage());
    }

    @Test
    public void namesACodeInTheMiddleOfTheTable() {
        assertEquals("DNS-SD Error -65548: NAME_CONFLICT",
                new AppleDNSSDException(DNSSDException.NAME_CONFLICT).getMessage());
        assertEquals("DNS-SD Error -65563: SERVICENOTRUNNING",
                new AppleDNSSDException(DNSSDException.SERVICENOTRUNNING).getMessage());
    }

    @Test
    public void namesTheLastKnownCode() {
        assertEquals("DNS-SD Error -65565: NATPORTMAPPINGDISABLED",
                new AppleDNSSDException(DNSSDException.NATPORTMAPPINGDISABLED).getMessage());
    }

    // Pins current behaviour: a code outside the table has no detail message, so the text
    // starts with "null". Cosmetic; see docs/runbooks/testing.md.
    @Test
    public void codeOutsideTheTableGetsNullPrefixedMessage() {
        assertEquals("null(-65566)", new AppleDNSSDException(-65566).getMessage());
        assertEquals("null(0)", new AppleDNSSDException(DNSSDException.NO_ERROR).getMessage());
    }
}
