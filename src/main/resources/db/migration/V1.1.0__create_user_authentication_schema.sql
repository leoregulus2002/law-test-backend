create table app_user (
    id bigint generated always as identity primary key,
    username varchar(64) not null,
    display_name varchar(64) not null,
    password_hash varchar(255) not null,
    webauthn_user_handle bytea not null,
    status varchar(32) not null default 'ACTIVE',
    failed_login_attempts integer not null default 0,
    locked_until timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint app_user_username_unique unique (username),
    constraint app_user_webauthn_user_handle_unique unique (webauthn_user_handle),
    constraint app_user_webauthn_user_handle_length check (octet_length(webauthn_user_handle) = 32),
    constraint app_user_failed_login_attempts_non_negative check (failed_login_attempts >= 0)
);

create table user_passkey (
    id bigint generated always as identity primary key,
    user_id bigint not null,
    credential_type varchar(32) not null,
    credential_id bytea not null,
    cose_public_key bytea not null,
    signature_count bigint not null,
    user_verified boolean not null,
    transports jsonb not null default '[]'::jsonb,
    backup_eligible boolean not null,
    backup_state boolean not null,
    aaguid bytea,
    attestation_object bytea,
    client_data_json bytea,
    label varchar(64) not null,
    created_at timestamp with time zone not null default current_timestamp,
    last_used_at timestamp with time zone,
    constraint user_passkey_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint user_passkey_credential_id_unique unique (credential_id),
    constraint user_passkey_user_label_unique unique (user_id, label),
    constraint user_passkey_signature_count_non_negative check (signature_count >= 0)
);

create index user_passkey_user_id_idx on user_passkey (user_id);

create table auth_ceremony (
    id uuid primary key,
    challenge bytea not null,
    ceremony_type varchar(32) not null,
    user_id bigint,
    options jsonb not null,
    is_dummy boolean not null default false,
    created_at timestamp with time zone not null default current_timestamp,
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    constraint auth_ceremony_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint auth_ceremony_challenge_length check (octet_length(challenge) = 32),
    constraint auth_ceremony_type_valid check (ceremony_type in ('REGISTER', 'AUTHENTICATE'))
);

create index auth_ceremony_expires_at_idx on auth_ceremony (expires_at);

create table refresh_session (
    id uuid primary key,
    user_id bigint not null,
    token_hash bytea not null,
    token_family_id uuid not null,
    previous_session_id uuid,
    created_at timestamp with time zone not null default current_timestamp,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    constraint refresh_session_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint refresh_session_previous_session_id_fkey
        foreign key (previous_session_id) references refresh_session (id),
    constraint refresh_session_token_hash_unique unique (token_hash),
    constraint refresh_session_token_hash_length check (octet_length(token_hash) = 32)
);

create index refresh_session_user_id_idx on refresh_session (user_id);
create index refresh_session_token_family_id_idx on refresh_session (token_family_id);
