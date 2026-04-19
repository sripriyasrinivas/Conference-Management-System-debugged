package com.cms.pattern.factory;

import com.cms.model.Certificate;
import com.cms.model.Conference;
import com.cms.model.User;
import org.springframework.stereotype.Component;

/** ConcreteCreator 3 — Best Paper Award */
@Component
public class BestPaperCertGenerator extends CertificateGenerator {

    @Override
    public Certificate.CertificateType getCertificateType() {
        return Certificate.CertificateType.BEST_PAPER;
    }

    @Override
    protected String buildTitle() {
        return "Best Paper Award";
    }

    @Override
    protected String buildBody(User recipient, Conference conference) {
        return "This award is proudly presented to " + recipient.getFullName()
                + " from " + institution(recipient)
                + " in recognition of the Best Paper at " + conference.getName()
                + ". This achievement reflects outstanding research and scholarly excellence.";
    }
}
