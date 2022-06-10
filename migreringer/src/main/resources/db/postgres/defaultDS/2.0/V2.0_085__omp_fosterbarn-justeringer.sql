
--fjerner foreign key som vi ikke vil ha
alter table omp_fosterbarn drop constraint if exists omp_fosterbarn_fosterbarna_id_fkey;

--fjerner unik indeks som sperrer for å bruke samme omp_fosterbarna også i revurdering
drop index if exists uidx_omp_gr_fosterbarn_2;
