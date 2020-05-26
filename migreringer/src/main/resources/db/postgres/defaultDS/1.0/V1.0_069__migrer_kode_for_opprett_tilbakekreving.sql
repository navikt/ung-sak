
update tilbakekreving_valg set videre_behandling = 'TILBAKEKR_OPPRETT' where videre_behandling = 'TILBAKEKR_INFOTRYGD';

update historikkinnslag_felt set til_verdi_kode = 'TILBAKEKR_OPPRETT' where til_verdi_kode = 'TILBAKEKR_INFOTRYGD'
