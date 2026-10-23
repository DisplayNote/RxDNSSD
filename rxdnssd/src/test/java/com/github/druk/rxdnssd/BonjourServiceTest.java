package com.github.druk.rxdnssd;

import org.junit.Test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The value object every rxdnssd stream emits. Parcelable is not covered (needs Android). */
public class BonjourServiceTest {

    private static final String NAME = "Living Room";
    private static final String TYPE = "_airplay._tcp.";
    private static final String DOMAIN = "local.";

    private static Inet4Address ipv4() throws UnknownHostException {
        return (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 10});
    }

    private static Inet6Address ipv6() throws UnknownHostException {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) 0xfe;
        bytes[1] = (byte) 0x80;
        bytes[15] = 1;
        return (Inet6Address) InetAddress.getByAddress(bytes);
    }

    @Test
    public void builderSetsEveryField() throws UnknownHostException {
        Map<String, String> txt = Collections.singletonMap("txtvers", "1");

        BonjourService service = new BonjourService.Builder(2, 3, NAME, TYPE, DOMAIN)
                .hostname("tv.local.")
                .port(7000)
                .dnsRecords(txt)
                .inet4Address(ipv4())
                .inet6Address(ipv6())
                .build();

        assertEquals(2, service.getFlags());
        assertEquals(3, service.getIfIndex());
        assertEquals(NAME, service.getServiceName());
        assertEquals(TYPE, service.getRegType());
        assertEquals(DOMAIN, service.getDomain());
        assertEquals("tv.local.", service.getHostname());
        assertEquals(7000, service.getPort());
        assertEquals(txt, service.getTxtRecords());
        assertEquals(ipv4(), service.getInet4Address());
        assertEquals(ipv6(), service.getInet6Address());
        assertEquals(2, service.getInetAddresses().size());
    }

    @Test
    public void unresolvedServiceHasNoHostPortRecordsOrAddresses() {
        BonjourService service = new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN).build();

        assertNull(service.getHostname());
        assertEquals(0, service.getPort());
        assertTrue(service.getTxtRecords().isEmpty());
        assertTrue(service.getInetAddresses().isEmpty());
        assertNull(service.getInet4Address());
        assertNull(service.getInet6Address());
    }

    @Test
    public void addressLookupsReturnTheFirstAddressOfEachFamily() throws UnknownHostException {
        Inet4Address second = (Inet4Address) InetAddress.getByAddress(new byte[]{10, 0, 0, 2});
        BonjourService.Builder builder = new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN);
        builder.inetAddress(ipv6());
        builder.inetAddress(ipv4());
        builder.inetAddress(second);

        BonjourService service = builder.build();

        assertEquals(ipv4(), service.getInet4Address());
        assertEquals(ipv6(), service.getInet6Address());
    }

    @Test
    public void isLostOnlyWhenTheLostFlagIsSet() {
        assertTrue(new BonjourService.Builder(BonjourService.LOST, 0, NAME, TYPE, DOMAIN).build().isLost());
        assertTrue(new BonjourService.Builder(BonjourService.LOST | 1, 0, NAME, TYPE, DOMAIN).build().isLost());
        assertFalse(new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN).build().isLost());
        assertFalse(new BonjourService.Builder(1, 0, NAME, TYPE, DOMAIN).build().isLost());
    }

    @Test
    public void equalityUsesNameTypeDomainAndInterfaceOnly() {
        BonjourService found = new BonjourService.Builder(0, 1, NAME, TYPE, DOMAIN).build();
        BonjourService lostAndResolved = new BonjourService.Builder(BonjourService.LOST, 1, NAME, TYPE, DOMAIN)
                .hostname("tv.local.").port(7000).build();

        assertEquals(found, lostAndResolved);
        assertEquals(found.hashCode(), lostAndResolved.hashCode());
    }

    @Test
    public void differentInterfaceNameTypeOrDomainIsADifferentService() {
        BonjourService service = new BonjourService.Builder(0, 1, NAME, TYPE, DOMAIN).build();

        assertNotEquals(service, new BonjourService.Builder(0, 2, NAME, TYPE, DOMAIN).build());
        assertNotEquals(service, new BonjourService.Builder(0, 1, "Kitchen", TYPE, DOMAIN).build());
        assertNotEquals(service, new BonjourService.Builder(0, 1, NAME, "_raop._tcp.", DOMAIN).build());
        assertNotEquals(service, new BonjourService.Builder(0, 1, NAME, TYPE, null).build());
        assertNotEquals(service, "Living Room");
    }

    @Test
    public void nullDomainsAreEqual() {
        BonjourService a = new BonjourService.Builder(0, 1, NAME, TYPE, null).build();
        BonjourService b = new BonjourService.Builder(0, 1, NAME, TYPE, null).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void copyBuilderKeepsEveryFieldAndDetachesTheRecords() throws UnknownHostException {
        Map<String, String> txt = new HashMap<>();
        txt.put("a", "1");
        BonjourService original = new BonjourService.Builder(4, 2, NAME, TYPE, DOMAIN)
                .hostname("tv.local.").port(7000).dnsRecords(txt).inet4Address(ipv4()).build();

        Map<String, String> more = new HashMap<>(original.getTxtRecords());
        more.put("b", "2");
        BonjourService copy = new BonjourService.Builder(original).inet6Address(ipv6()).build();
        BonjourService replaced = new BonjourService.Builder(original).dnsRecords(more).build();

        assertEquals(4, copy.getFlags());
        assertEquals("tv.local.", copy.getHostname());
        assertEquals(7000, copy.getPort());
        assertEquals(original.getTxtRecords(), copy.getTxtRecords());
        assertEquals(ipv4(), copy.getInet4Address());
        assertEquals(ipv6(), copy.getInet6Address());
        assertNull(original.getInet6Address());
        assertEquals(1, original.getTxtRecords().size());
        assertEquals(2, replaced.getTxtRecords().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void txtRecordsCannotBeModifiedThroughTheService() {
        BonjourService service = new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN).build();

        service.getTxtRecords().put("a", "1");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void addressesCannotBeModifiedThroughTheService() throws UnknownHostException {
        BonjourService service = new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN).build();

        service.getInetAddresses().add(ipv4());
    }

    // Pins current behaviour: dnsRecords(map) keeps the caller's map, so changing it after
    // build() changes the service. Suspected bug; see docs/runbooks/testing.md.
    @Test
    public void builtServiceSharesTheMapPassedToDnsRecords() {
        Map<String, String> txt = new HashMap<>();
        BonjourService service = new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN).dnsRecords(txt).build();

        txt.put("a", "1");

        assertEquals("1", service.getTxtRecords().get("a"));
    }

    @Test
    public void builderMethodsChain() {
        BonjourService.Builder builder = new BonjourService.Builder(0, 0, NAME, TYPE, DOMAIN);

        assertSame(builder, builder.hostname("h").port(1).dnsRecords(new HashMap<String, String>()));
    }

    @Test
    public void toStringListsTheFields() {
        BonjourService service = new BonjourService.Builder(2, 3, NAME, TYPE, DOMAIN)
                .hostname("tv.local.").port(7000).dnsRecords(Collections.singletonMap("a", "1")).build();

        assertEquals("BonjourService{flags=2, domain='local.', regType='_airplay._tcp.', "
                + "serviceName='Living Room', dnsRecords={a=1}, ifIndex=3, hostname='tv.local.', port=7000}",
                service.toString());
    }
}
