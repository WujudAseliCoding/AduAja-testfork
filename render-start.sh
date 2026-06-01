#!/bin/bash

echo "Memulai proses restore database dari Dropbox..."

# 1. Dapatkan Access Token baru menggunakan Refresh Token
RESPONSE=$(curl -s -X POST https://api.dropbox.com/oauth2/token \
  -u "$DROPBOX_APP_KEY:$DROPBOX_APP_SECRET" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=$DROPBOX_REFRESH_TOKEN")

# 2. Ekstrak token menggunakan regex bawaan Linux
FRESH_TOKEN=$(echo $RESPONSE | grep -o '"access_token": *"[^"]*"' | sed 's/"access_token": *"//' | sed 's/"//')

if [ -z "$FRESH_TOKEN" ]; then
    echo "Gagal menarik database dari Dropbox (Mungkin ini deployment pertama atau token salah). Lanjut menyalakan aplikasi."
else
    echo "Berhasil terhubung ke awan. Mengunduh database..."

    # Pastikan folder data tersedia
    mkdir -p data

    # 3. Unduh file zip backup dari Dropbox
    curl -X POST https://content.dropboxapi.com/2/files/download \
      --header "Authorization: Bearer $FRESH_TOKEN" \
      --header "Dropbox-API-Arg: {\"path\": \"/aduaja_backup.zip\"}" \
      -o data/aduaja_backup.zip || true

    # 4. Ekstrak file zip dengan aman menggunakan JAR tools
    if [ -f data/aduaja_backup.zip ]; then
        echo "Mengekstrak file backup..."
        cd data
        jar xf aduaja_backup.zip
        rm aduaja_backup.zip
        cd ..
        echo "Database berhasil dipulihkan!"
    fi
fi

echo "Menyalakan mesin aplikasi Spring Boot AduAja..."
# 5. Jalankan file .jar hasil kompilasi Docker di port wajib Hugging Face
java -jar app.jar --server.port=7860