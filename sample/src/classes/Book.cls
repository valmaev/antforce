public class Book {
    
    private final Id m_uniqueId;
    private final String m_title;
    private final String m_description;
    
    public Book (Id uniqueId, String title, String description) {
        if (uniqueId == null)
            throw new ArgumentNullException(
                'Parameter "uniqueId" can\'t be null');
        if (title == null)
            throw new ArgumentNullException(
                'Parameter "title" can\'t be null');
        if (description == null)
            throw new ArgumentNullException(
                'Parameter "description" can\'t be null');
        
        m_uniqueId = uniqueId;
        m_title = title;
        m_description = description;
    }
    
    public Id UniqueId { get { return m_uniqueId; } }
    public String Title { get { return m_title; } }
    public String Description { get { return m_description; } }

    public override String toString() {
        return String.format(
            'UniqueId: {0}, Title: {1}, Description: {2}', 
            new String[] { m_uniqueId, m_title, m_description});
    }
}