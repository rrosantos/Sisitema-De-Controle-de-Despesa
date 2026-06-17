package br.com.controledespesas.view;

record SelectionOption<T>(T value, String label) {

    @Override
    public String toString() {
        return label;
    }
}
