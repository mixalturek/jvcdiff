package net.dongliu.vcdiff.diff;

import net.dongliu.vcdiff.vc.AddressCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * @author dongliu
 */
public class AddressCacheTest {
    AddressCache addressCache;

    @Before
    public void setUp() throws Exception {
        addressCache = new AddressCache();
    }

    @Test
    public void test() throws IOException {
        int[] encodeAddress = new int[1];
        short mode = addressCache.encodeAddress(100, 200, encodeAddress);
        Assert.assertEquals(200 - 100, encodeAddress[0]);
        Assert.assertEquals(0, mode);
    }
}
