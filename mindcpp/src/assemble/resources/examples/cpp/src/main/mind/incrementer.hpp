#ifndef incrementer_HPP_
#define incrementer_HPP_

#include "incrementer.adl.hpp"

using namespace __ns_incrementer;

class incrementer: public incrementerDefinition {
protected:
public:
    virtual void start(void);
    virtual void stop(void);
    virtual void increment(uint32_t input);
    virtual void twice(uint32_t input);
    virtual void sqrt(uint32_t input);
};

#endif /* incrementer_HPP_ */

