// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
import storybook from "eslint-plugin-storybook"

import { fixupConfigRules } from "@eslint/compat"
import react from "eslint-plugin-react"
import reactRefresh from "eslint-plugin-react-refresh"
import html from "eslint-plugin-html"
import prettier from "eslint-plugin-prettier"
import globals from "globals"
import tsParser from "@typescript-eslint/parser"
import path from "node:path"
import { fileURLToPath } from "node:url"
import js from "@eslint/js"
import { FlatCompat } from "@eslint/eslintrc"

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
})

export default [
  {
    ignores: ["**/dist", "**/.eslintrc.cjs"],
  },
  ...fixupConfigRules(compat.extends("eslint:recommended", "plugin:@typescript-eslint/recommended", "plugin:react-hooks/recommended", "prettier")),
  {
    plugins: {
      react,
      "react-refresh": reactRefresh,
      html,
      prettier,
    },

    languageOptions: {
      globals: {
        ...globals.browser,
      },

      parser: tsParser,
    },

    rules: {
      "react-refresh/only-export-components": [
        "off",
        {
          allowConstantExport: true,
        },
      ],
      "react/jsx-curly-brace-presence": ["error", { props: "always" }],
      "prettier/prettier": "warn",
    },
  },
  ...storybook.configs["flat/recommended"],
]
