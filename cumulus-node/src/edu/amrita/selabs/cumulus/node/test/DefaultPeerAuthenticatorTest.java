package edu.amrita.selabs.cumulus.node.test;

import static org.junit.Assert.*;

import java.security.Key;

import javax.crypto.Cipher;

import org.junit.Test;
import edu.amrita.selabs.cumulus.lib.RSACryptoUtil;
import edu.amrita.selabs.cumulus.lib.RSAKeyUtil;
import edu.amrita.selabs.cumulus.lib.StringUtil;
import edu.amrita.selabs.cumulus.node.DefaultPeerAuthenticator;
import static edu.amrita.selabs.cumulus.node.PeerAuthenticator.Status;

public class DefaultPeerAuthenticatorTest {

	String sPubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoZg8uP8T3Zc2i27eTl5q/msw1HyG5TEb1CjljmeB2x/RbpOsPri89tlMY0Q5LyCV/u+KutHRl8HCWM44RJjfm/bLyqjXPav9EhXMCX6PV9zkHrfQBe+DounBNB36YyC6StyyJ09k1+yVOAMunVVBESIdX8ChYcE3iGPzbRNky9IA3I7tN7TKk1RQaRLRghESacVduQfsTaoLwXN0w52k8rcBu9Cmd7kVJ8JjMlkLyg+n8c6u+VjyPnr/bezASaW7zx6LhQF9zkpmBZfXaNE+DDHrpcLP9PR3N738xQlcFHxK9k7K5KZaw/iONqDdT1aA6aD82bGrLvJuPQSSYuyQMQIDAQAB";
	
	String sPrivKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQChmDy4/xPdlzaLbt5OXmr+azDUfIblMRvUKOWOZ4HbH9Fuk6w+uLz22UxjRDkvIJX+74q60dGXwcJYzjhEmN+b9svKqNc9q/0SFcwJfo9X3OQet9AF74Oi6cE0HfpjILpK3LInT2TX7JU4Ay6dVUERIh1fwKFhwTeIY/NtE2TL0gDcju03tMqTVFBpEtGCERJpxV25B+xNqgvBc3TDnaTytwG70KZ3uRUnwmMyWQvKD6fxzq75WPI+ev9t7MBJpbvPHouFAX3OSmYFl9do0T4MMeulws/09Hc3vfzFCVwUfEr2TsrkplrD+I42oN1PVoDpoPzZsasu8m49BJJi7JAxAgMBAAECggEBAI+2AGrWDyjGzi96ziIMPkM9uROAG8fAwOsR4/WdgBMMXQlmM40kZolx/0lY5zJOXJd5GXvEFg5MtzabT9dRzau69MqrFaE3T73BvM/Eyl5tiEL5XfupYvyDvttGveNiu8rQM3BaX2jYcmx6B/7MTy2ZlV4OxndVirm1iku7L61DDW+t4d72otgJUGYY85GaVNwpRXMjwUU31Qmay/nciGphNC8v1K1jSHAgcaAIP0pgCRcK5Q7FakdkyoF2eczoN5osnqvLPy9aNjqvxlntZX2us3MRbuWpbQwGOMqrNU3/R+Si4z8BQmU4hN2e9CMq2A6AqvXSFxpsFRR9ws9NdfECgYEA6TbBqde5Lbzisig5eabP6dAQfYeM5BMcluoBN2yqHTLt7ncIFxMjvsWHRwmbOaZVpfVYqSB5p8zNBxHHoDJAEepEK+o7vNIUPsp3JDr7xIfZIpo612cfZF40WCVd/x42OCB+Jc7nUHTEmC+1S5FKN0VFluJ6f2ZszUJhhBi5MoMCgYEAsWIcY0bbVIdmSL8zSjYqn8dOgsa+JPwx4EnE9zxibyy5f2ww4uTJGw4onpuirfQDAojtKKTxzdcTP3hN1hfGWCtbZcUAEgCvfxKfPvOg4A33D49QO6MSbkXxR6TVr/QOtHRCSFjAtAQ0fxwTARqW0uLh5rgRQLS1I1JvXtb2pDsCgYB8vWsM23QTAMsIm/dSGGVxHHcVjaFQhOBv4C5SZO8AuzMEr+pF3VjrO2BGIN9EELITGI4ZuGMZ27N2qo3qI7adXn6kNp9JOdzWURk06c+sqTT9Cx2aBXCHENRSqXoDcTdzAf43Xtne8PYOqMYB626U7ZpxQCZhanmbuHvMWiUE3QKBgD6kTzIgC8TGLDD0lMBYcKUMVYWJegzFozd51b0Z7gkk2j1Nj9YQxZRwY5ffjouv6IA0qsv3tAzlLz7y1UoOC1WHujcuYt/4rgFKKkKo9t8pD17HVaY56IBY4uRSKcSBg8/mXqH99c+czI029N8m7Kx8wxofVrhrGKzop7vuXhuRAoGAMyzqKr2+6p5Q6OzieFrcrLrx9t00O5MTAI/yvZQ2bSOFJef9GW+wijf88tcyNoYnZpsXRYDeO3TdI4CTa5YN3U+5hBe6yinrGlqxo2jQQ1HaMGK+DsItA0YGLuXRJ4dVIYb9ix67rrM36JbPv2p46bbKdvaomvzC6JqDbqn/6oI=";
	
	@Test
	public void test() throws Exception{
		
		String bid = "1234567890";
		String nid = "1234";
		
		DefaultPeerAuthenticator auth = new DefaultPeerAuthenticator();
		
		auth.addNode(nid,  sPubKey);

		RSAKeyUtil keyUtil = new RSAKeyUtil();
		Key privKey = keyUtil.decodePrivateKey(sPrivKey);
		
		RSACryptoUtil cUtil = new RSACryptoUtil();
		
		String sign = cUtil.sign(bid,  privKey);
		
		assertTrue(auth.doAuth(nid,  bid,  sign) == Status.SUCCESS);
		
	}

}
