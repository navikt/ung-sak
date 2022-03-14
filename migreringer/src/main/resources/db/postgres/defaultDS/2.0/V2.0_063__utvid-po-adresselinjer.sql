-- Utvider adresser - PDLs adresselinjer kan være opp til 4000 tegn teknisk :(
ALTER TABLE po_adresse
    ALTER COLUMN adresselinje1 TYPE varchar(1000),
    ALTER COLUMN adresselinje2 TYPE varchar(1000),
    ALTER COLUMN adresselinje3 TYPE varchar(1000),
    ALTER COLUMN adresselinje4 TYPE varchar(1000)
    ;
