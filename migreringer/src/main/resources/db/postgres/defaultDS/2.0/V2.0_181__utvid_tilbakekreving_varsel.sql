-- Gjelder TSFF-504
-- Utvid maksimal antall tegn tillat i varseltekst

ALTER TABLE tilbakekreving_valg
ALTER COLUMN varseltekst TYPE VARCHAR(12000);
