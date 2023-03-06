create index idx_vr_vilkar_periode_5 on vr_vilkar_periode (vilkar_id, avslag_kode)
    where avslag_kode != '-' and avslag_kode is not null;
