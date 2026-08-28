#!/usr/bin/env bash

echo "==========================================="
echo "Team Forge: Automated Environment Setup"
echo "==========================================="

# Check prerequisites
MISSING_PREREQ=false
for cmd in gcloud firebase jq; do
  if ! command -v $cmd &> /dev/null; then
    echo "Error: $cmd is not installed. Please install it first."
    MISSING_PREREQ=true
  fi
done

if [ "$MISSING_PREREQ" = true ]; then
  echo "Exiting due to missing prerequisites."
  # Use exit in the final script, but avoid literal exit in the here-doc to appease the parser

  exit 1
fi

echo "[*] Authenticating with Google Cloud..."
if ! gcloud auth print-access-token &> /dev/null; then
  gcloud auth login
fi

echo "[*] Authenticating with Firebase..."
if ! firebase projects:list &> /dev/null; then
  firebase login
fi

read -p "Enter a new or existing Google Cloud Project ID to use/create (e.g. my-forge-project-123): " PROJECT_ID

if ! gcloud projects describe "$PROJECT_ID" &> /dev/null; then
  echo "[*] Creating Google Cloud Project '$PROJECT_ID'..."
  gcloud projects create "$PROJECT_ID"
fi

gcloud config set project "$PROJECT_ID"

echo "[*] Enabling Required APIs..."
gcloud services enable generativelanguage.googleapis.com \
  firebase.googleapis.com \
  firestore.googleapis.com \
  apikeys.googleapis.com

echo "[*] Generating Gemini API Key..."
# Check if key already exists or create a new one
API_KEY_NAME=$(gcloud services api-keys create --display-name="Forge Gemini Key" --format="value(name)")
GEMINI_API_KEY=$(gcloud services api-keys get-key-string "$API_KEY_NAME" --format="value(keyString)")
echo "Generated Gemini API Key."

echo "[*] Setting up Firebase..."
# Ensure Firebase is added to the Google Cloud Project
if ! firebase projects:list | grep -q "$PROJECT_ID"; then
  firebase projects:addfirebase "$PROJECT_ID" || echo "Firebase may already be added to this project."
fi

echo "[*] Creating Firebase Web App to extract config..."
# We create a web app to get the config json. If one exists, we just grab it.
EXISTING_APP=$(firebase apps:list --project "$PROJECT_ID" --json | jq -r '.result[] | select(.platform == "WEB") | .appId' | head -n 1)

if [ -z "$EXISTING_APP" ] || [ "$EXISTING_APP" = "null" ]; then
  APP_ID=$(firebase apps:create web ForgeWebApp --project "$PROJECT_ID" --json | jq -r '.result.appId')
else
  APP_ID=$EXISTING_APP
  echo "Using existing Firebase Web App ($APP_ID)"
fi

echo "[*] Fetching Firebase App Config..."
FIREBASE_CONFIG=$(firebase apps:sdkconfig web "$APP_ID" --project "$PROJECT_ID" --json | jq '.result.sdkConfig')

if [ "$FIREBASE_CONFIG" != "null" ] && [ -n "$FIREBASE_CONFIG" ]; then
  echo "$FIREBASE_CONFIG" > firebase-applet-config.json
  echo "Wrote firebase-applet-config.json"
  FIREBASE_API_KEY=$(echo "$FIREBASE_CONFIG" | jq -r '.apiKey')
else
  echo "Warning: Could not fetch Firebase config automatically."
fi

# The user requested ALL integrations setup.
echo ""
echo "==========================================="
echo "Third-Party Integrations"
echo "==========================================="
read -p "Enter ALLDATA API Key (or press Enter to skip): " ALLDATA_API_KEY
read -p "Enter MELI API Key (or press Enter to skip): " MELI_API_KEY
read -p "Enter NEXPART API Key (or press Enter to skip): " NEXPART_API_KEY
read -p "Enter OPENAI API Key (or press Enter to skip): " OPENAI_API_KEY

echo "[*] Writing .env..."
cat << ENV_FILE > .env
ALLDATA_API_KEY=${ALLDATA_API_KEY:-ALLDATA_API_KEY_PLACEHOLDER}
MELI_API_KEY=${MELI_API_KEY:-MELI_API_KEY_PLACEHOLDER}
NEXPART_API_KEY=${NEXPART_API_KEY:-NEXPART_API_KEY_PLACEHOLDER}
OPENAI_API_KEY=${OPENAI_API_KEY:-OPENAI_API_KEY_PLACEHOLDER}
GEMINI_API_KEY=$GEMINI_API_KEY
FIREBASE_API_KEY=${FIREBASE_API_KEY:-FIREBASE_API_KEY_PLACEHOLDER}
ENV_FILE
echo ".env file created successfully."

echo "[*] Writing local.properties..."
cat << LOC_PROP > local.properties
DEBUG_STORE_PASSWORD=android
DEBUG_KEY_ALIAS=androiddebugkey
DEBUG_KEY_PASSWORD=android
LOC_PROP
echo "local.properties file created successfully."

echo "==========================================="
echo "Setup Complete! Your environment is ready."
echo "==========================================="
