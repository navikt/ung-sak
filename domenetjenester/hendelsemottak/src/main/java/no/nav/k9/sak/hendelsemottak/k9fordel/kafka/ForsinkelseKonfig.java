package no.nav.k9.sak.hendelsemottak.k9fordel.kafka;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.felles.konfigurasjon.env.Environment;


/**
 * Vi ønsker ikke forsinkelser når applikasjonen kjører lokalt,
 * både ved manuell testing og automatisert igjennom Autotest.
 */
@ApplicationScoped
public class ForsinkelseKonfig {
    private static final Environment ENV = Environment.current();

}
