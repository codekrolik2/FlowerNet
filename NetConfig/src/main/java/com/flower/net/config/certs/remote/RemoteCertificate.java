package com.flower.net.config.certs.remote;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.flower.net.config.certs.BksFileConfig;
import com.flower.net.config.certs.CertificatePkcs11Config;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/** One of */
@Value.Immutable
@JsonSerialize(as = ImmutableRemoteCertificate.class)
@JsonDeserialize(as = ImmutableRemoteCertificate.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface RemoteCertificate {
    @JsonProperty
    @Nullable CertificatePkcs11Config pkcs11Config();

    @JsonProperty
    @Nullable RemoteCertificateFileConfig fileConfig();

    @JsonProperty
    @Nullable RemoteCertificateResourceConfig resourceConfig();

    @JsonProperty
    @Nullable
    BksFileConfig bksConfig();
}
