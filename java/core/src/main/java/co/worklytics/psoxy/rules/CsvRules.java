package co.worklytics.psoxy.rules;

import com.avaulta.gateway.rules.ColumnarRules;
import com.avaulta.gateway.rules.RuleSet;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;


@AllArgsConstructor //for builder
//@NoArgsConstructor //for Jackson
@SuperBuilder
public class CsvRules extends ColumnarRules implements RuleSet {

}
