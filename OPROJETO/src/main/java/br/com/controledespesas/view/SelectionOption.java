package br.com.controledespesas.view;

/**
 * Transporta dados imutaveis usados no fluxo de SelectionOption.
 */
record SelectionOption<T>(T value, String label) {

    @Override
    public String toString() {
        return label;
    }
}
