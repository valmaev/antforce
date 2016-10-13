@IsTest
private class ContactTriggerTestClass {

    @IsTest
    private static void beforeInsert_always_shouldNotThrow() {
        Account account = new Account(Name = 'foo');
        insert account;
        insert new Contact(LastName = 'Bar', AccountId = account.Id);
    }
}