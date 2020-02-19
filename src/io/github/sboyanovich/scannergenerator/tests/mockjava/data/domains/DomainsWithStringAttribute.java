package io.github.sboyanovich.scannergenerator.tests.mockjava.data.domains;

import io.github.sboyanovich.scannergenerator.scanner.Fragment;
import io.github.sboyanovich.scannergenerator.scanner.Text;
import io.github.sboyanovich.scannergenerator.scanner.token.DomainWithAttribute;
import io.github.sboyanovich.scannergenerator.scanner.token.TokenWithAttribute;
import io.github.sboyanovich.scannergenerator.tests.mockjava.data.tokens.TComment;
import io.github.sboyanovich.scannergenerator.utility.Utility;

public enum DomainsWithStringAttribute implements DomainWithAttribute<String> {
    COMMENT {
        @Override
        public TokenWithAttribute<String> createToken(Text text, Fragment fragment) {
            return new TComment(fragment, attribute(text, fragment));
        }
    };

    @Override
    public String attribute(Text text, Fragment fragment) {
        return Utility.getTextFragmentAsString(text, fragment);
    }
}