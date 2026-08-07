# Bourbon Programming Language Antora UI Theme

This is the dedicated, standalone Antora user interface theme for the **Bourbon Programming Language**.
It implements a warm, premium, low-eye-strain design system called the **Cacao, Honey, and Cream** visual system, inspired by the Bourbon Vanilla Orchid logo.

## Design Highlights

- **Visual Brand Metaphor:** High-contrast warm cream canvas, rich ambers, golden honey accents, and deep dark chocolate/roasted espresso.
- **Dual-Mode System:** Out-of-the-box support for light mode (Toasted Cream & Honey) and automatic system-responsive dark mode (Roasted Espresso & Gold).
- **Typography:** Sleek geometric sans-serif headings (`Space Grotesk`) and elegant, readable editorial-grade serifs for body copy (`Lora`).
- **Warm Syntax Highlighting:** A custom-made restrained palette for monospaced code blocks.

## Directory Structure

```
bourbon-ui/
├── css/
│   ├── site.css       # Standard Antora stylesheet
│   └── custom.css     # Our custom brand styles & overrides
├── partials/
│   └── head-styles.hbs # Header template linking fonts & custom.css
├── ui.yml             # Theme metadata
├── package.json       # Project configurations and build scripts
└── README.md          # This file
```

## Building the Bundle

To package this theme into a static `.zip` bundle for Antora, run:

```bash
npm run build
```

This creates a `build/ui-bundle.zip` archive containing the bundled assets.

## Integration in Antora Playbook

To use this local UI theme project in your main Antora documentation playbook, update the `ui` section of your `antora-playbook.yml` file:

```yaml
ui:
  bundle:
    url: ./bourbon-ui/build/ui-bundle.zip
```

Alternatively, during active development, you can point directly to the unpacked directory to preview styles without zipping:

```yaml
ui:
  bundle:
    url: ./bourbon-ui
```
