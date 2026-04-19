package com.cms.pattern.factory;

import com.cms.model.Certificate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Client / Dispatcher for the Factory Method pattern.
 *
 * Spring injects all CertificateGenerator beans automatically.
 * The factory maps each Certificate.CertificateType to its
 * corresponding generator — callers just call getGenerator(type).
 *
 * DESIGN PRINCIPLE (Open/Closed):
 *   To add a new certificate type, you only add a new
 *   CertificateGenerator subclass annotated with @Component.
 *   This class and CertificateServiceImpl need ZERO changes.
 */
@Component
public class CertificateGeneratorFactory {

    private final Map<Certificate.CertificateType, CertificateGenerator> registry;

    /** Spring auto-injects all CertificateGenerator beans */
    public CertificateGeneratorFactory(List<CertificateGenerator> generators) {
        this.registry = generators.stream()
                .collect(Collectors.toMap(
                        CertificateGenerator::getCertificateType,
                        Function.identity()
                ));
    }

    /**
     * Returns the appropriate CertificateGenerator for the given type.
     *
     * @param type the certificate type requested
     * @return the concrete CertificateGenerator for that type
     * @throws IllegalArgumentException if no generator is registered
     */
    public CertificateGenerator getGenerator(Certificate.CertificateType type) {
        CertificateGenerator generator = registry.get(type);
        if (generator == null) {
            throw new IllegalArgumentException(
                    "No certificate generator registered for type: " + type);
        }
        return generator;
    }
}
