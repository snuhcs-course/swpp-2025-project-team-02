#!/bin/bash

# Django Code Diagram Generation Script
# Requires: django-extensions, pydot, pylint (for pyreverse), pydeps, graphviz

set -e

echo "🎨 Generating Django code diagrams..."

# Create diagrams directory
DIAGRAM_DIR="diagrams"
mkdir -p "$DIAGRAM_DIR"

# 1. Generate full model diagram (all apps)
echo "📊 Generating full model diagram..."
python manage.py graph_models \
    -a \
    -g \
    -o "$DIAGRAM_DIR/all_models.png" \
    --arrow-shape normal

# 2. Generate model diagram excluding built-in Django apps
echo "📊 Generating project model diagram (excluding Django built-ins)..."
python manage.py graph_models \
    core user \
    -g \
    -o "$DIAGRAM_DIR/project_models.png" \
    --arrow-shape normal

# 3. Generate model diagram with attributes
echo "📊 Generating detailed model diagram..."
python manage.py graph_models \
    core user \
    -o "$DIAGRAM_DIR/detailed_models.png" \
    --arrow-shape normal

# 4. Generate SVG version for better quality
echo "📊 Generating SVG diagrams..."
python manage.py graph_models \
    core user \
    -g \
    -o "$DIAGRAM_DIR/project_models.svg" \
    --arrow-shape normal

# 5. Generate class diagrams including Serializers and Views (pyreverse)
echo "🔷 Generating class diagrams (includes Serializers, Views)..."
pyreverse -o png -p core_classes core/ -d "$DIAGRAM_DIR" 2>/dev/null || echo "⚠️  pyreverse for core failed (install: pip install pylint)"
pyreverse -o png -p user_classes user/ -d "$DIAGRAM_DIR" 2>/dev/null || echo "⚠️  pyreverse for user failed (install: pip install pylint)"

# 6. Generate module dependency graphs (pydeps)
echo "🔗 Generating module dependency graphs..."
pydeps core --max-bacon 2 -o "$DIAGRAM_DIR/core_dependencies.png" 2>/dev/null || echo "⚠️  pydeps for core failed (install: pip install pydeps)"
pydeps user --max-bacon 2 -o "$DIAGRAM_DIR/user_dependencies.png" 2>/dev/null || echo "⚠️  pydeps for user failed (install: pip install pydeps)"

echo ""
echo "✅ Diagrams generated successfully in '$DIAGRAM_DIR/' directory!"
echo ""
echo "Generated files:"
echo "  📊 Models:"
echo "    - all_models.png (all apps including Django built-ins)"
echo "    - project_models.png (project apps only, grouped)"
echo "    - detailed_models.png (project apps with all fields)"
echo "    - project_models.svg (scalable vector version)"
echo ""
echo "  🔷 Classes (Serializers, Views, etc):"
echo "    - classes_core_classes.png (core app class diagram)"
echo "    - classes_user_classes.png (user app class diagram)"
echo "    - packages_core_classes.png (core app package diagram)"
echo "    - packages_user_classes.png (user app package diagram)"
echo ""
echo "  🔗 Dependencies:"
echo "    - core_dependencies.png (core module dependencies)"
echo "    - user_dependencies.png (user module dependencies)"
