trigger ContactTrigger on Contact (before insert, after insert, after update) {
    Contact newContact = new Contact();
}