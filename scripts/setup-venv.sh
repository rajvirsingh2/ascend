#!/bin/bash

# Exit on error
set -e

# Navigate to the rag-service directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RAG_DIR="$PROJECT_ROOT/rag-service"

cd "$RAG_DIR"

echo "🚀 Creating virtual environment in $RAG_DIR/.venv..."
python3 -m venv .venv

echo "🔄 Activating virtual environment..."
source .venv/bin/activate

echo "📦 Upgrading pip..."
pip install --upgrade pip

echo "📥 Installing dependencies from requirements.txt..."
pip install -r requirements.txt

echo "✅ Setup complete!"
echo ""
echo "To activate the environment, run:"
echo "source rag-service/.venv/bin/activate"
