package test;

public class TestString {
    public static void main(String[] args) {
        String str = "ААВваааАВА, ,  ап.па FD .FD f. dfdf !~@#$%^&*()_-++=@$%%#^^* ававаопоПВПВ";

        str = str.toLowerCase();

        String strRU = str.replaceAll("[^а-я ]*", "").trim();
        String strEN = str.replaceAll("[^a-z ]*", "").trim();

        String[] wordsRU = strRU.split("[\s]+");
        String[] wordsEN = strEN.split("[\s]+");

        for (int i = 0; i < wordsRU.length; i++) {
            System.out.println(wordsRU[i]);
        }

        for (int i = 0; i < wordsEN.length; i++) {
            System.out.println(wordsEN[i]);
        }
    }
}