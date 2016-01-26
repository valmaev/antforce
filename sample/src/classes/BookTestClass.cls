@IsTest
public class BookTestClass {

    private static final Id m_dummyId = '120000000000000001';
    private static final String m_dummyTitle = 'foo';
    private static final String m_dummyDescription = 'bar';

    @IsTest
    private static void constructor_withNullId_shouldThrow() {
        try {
            new BookBuilder().withUniqueId(null).build();
        } catch (ArgumentNullException ex) {
            System.assert(ex.getMessage().contains('uniqueId'));
            System.assert(ex.getMessage().contains('can\'t be null'));
            return;
        }

        System.assert(true == false);
    }

    @IsTest
    private static void constructor_withNullTitle_shouldThrow() {
        try {
            new BookBuilder().withTitle(null).build();
        } catch (ArgumentNullException ex) {
            System.assert(ex.getMessage().contains('title'));
            System.assert(ex.getMessage().contains('can\'t be null'));
            return;
        }

        System.assert(true == false);
    }


    @IsTest
    private static void constructor_withNullDescription_shouldThrow() {
        try {
            new BookBuilder().withDescription(null).build();
        } catch (ArgumentNullException ex) {
            System.assert(ex.getMessage().contains('description'));
            System.assert(ex.getMessage().contains('can\'t be null'));
            return;
        }

        System.assert(true == false);
    }

    @IsTest
    private static void uniqueId_always_shouldProperlyInitialized() {
        Book sut = new BookBuilder().withUniqueId(m_dummyId).build();
        System.assertEquals(m_dummyId, sut.UniqueId);
    }

    @IsTest
    private static void title_always_shouldProperlyInitialized() {
        Book sut = new BookBuilder().withTitle(m_dummyTitle).build();
        System.assertEquals(m_dummyTitle, sut.Title);
    }

    @IsTest
    private static void description_always_shouldProperlyInitialized() {
        Book sut = new BookBuilder().withDescription(m_dummyDescription).build();
        System.assertEquals(m_dummyDescription, sut.Description);
    }

    @IsTest
    private static void toString_always_shouldReturnExpectedResult() {
        Book sut = new BookBuilder()
            .withUniqueId(m_dummyId)
            .withTitle(m_dummyTitle)
            .withDescription(m_dummyDescription)
            .build();

        String actual = sut.toString();

        System.assert(actual.contains(m_dummyId));
        System.assert(actual.contains(m_dummyTitle));
        System.assert(actual.contains(m_dummyDescription));
    }
}