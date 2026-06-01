#!/bin/bash

echo "Memulai proses restore database dari Dropbox..."

# Dapatkan Access Token baru
RESPONSE=$(curl -s -X POST https://api.dropbox.com/oauth2/token \
  -u "$DROPBOX_APP_KEY:$DROPBOX_APP_SECRET" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=$DROPBOX_REFRESH_TOKEN")

# Ekstrak token dengan regex bawaan Linux
FRESH_TOKEN=$(echo $RESPONSE | grep -o '"access_token": *"[^"]*"' | sed 's/"access_token": *"//' | sed 's/"//')

if [ -z "$FRESH_TOKEN" ]; then
    echo "Gagal menarik database dari Dropbox (Mungkin ini deployment pertama). Lanjut menyalakan aplikasi."
else
    echo "Berhasil terhubung ke awan. Mengunduh database..."
    mkdir -p data

    # Unduh file zip
    curl -X POST https://content.dropboxapi.com/2/files/download \
      --header "Authorization: Bearer $FRESH_TOKEN" \
      --header "Dropbox-API-Arg: {\"path\": \"/aduaja_backup.zip\"}" \
      -o data/aduaja_backup.zip || true

    # Ekstrak file zip dengan aman menggunakan JAR tools
    if [ -f data/aduaja_backup.zip ]; then
        echo "Mengekstrak file backup..."
        cd data
        jar xf aduaja_backup.zip
        rm aduaja_backup.zip
        cd ..
        echo "Database berhasil dipulihkan!"
    fi
fi

echo "Menyalakan mesin aplikasi Spring Boot..."
./mvnw spring-boot:run