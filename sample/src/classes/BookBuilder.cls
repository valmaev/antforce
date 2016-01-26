public class BookBuilder {
    private Id m_uniqueId = '000000000000000001';
    private String m_title = '';
    private String m_description = '';

    public BookBuilder withUniqueId(Id value) {
        m_uniqueId = value;
        return this;
    }

    public BookBuilder withTitle(String value) {
        m_title = value;
        return this;
    }

    public BookBuilder withDescription(String value) {
        m_description = value;
        return this;
    }

    public Book build() {
        return new Book(m_uniqueId, m_title, m_description);
    }
}