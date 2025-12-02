package com.garganttua.keys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum GGEncryptionMode {
	ECB,
    CBC,
    CFB,
    OFB,
    GCM,
    CTR,
	NONE,
	ECDSA;

}
