package com.garganttua.core.crypto;

import java.util.Date;

import com.garganttua.core.dsl.IBuilder;

public interface IKeyRealmBuilder extends IBuilder<IKeyRealm> {

	IKeyRealmBuilder name(String name);

	IKeyRealmBuilder algorithm(IKeyAlgorithm algorithm);

	IKeyRealmBuilder expiration(Date expiration);

	IKeyRealmBuilder initializationVectorSize(int size);

	IKeyRealmBuilder encryptionMode(EncryptionMode mode);

	IKeyRealmBuilder paddingMode(EncryptionPaddingMode paddingMode);

	IKeyRealmBuilder signatureAlgorithm(SignatureAlgorithm signatureAlgorithm);

}
